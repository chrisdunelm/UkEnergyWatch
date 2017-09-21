using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public abstract class DataStoreWriter<T, TFactory> : DataStore where T : IStorable<T, TFactory> where TFactory : IStorableFactory<T>, new()
    {
        public DataStoreWriter(ITaskHelper taskHelper, IDirectory dir, string prefix)
        {
            _taskHelper = taskHelper;
            _dir = dir;
            _prefix = prefix;
            _factory = Singletons<TFactory>.Instance;
            // Find last file, and check to see if it's version-compatible
            _appendFileNameTask = _taskHelper.Run(() => AppendFileName(CancellationToken.None));
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IDirectory _dir;
        private readonly string _prefix;
        private readonly TFactory _factory;

        private Task<(long count, FileName filename)> _appendFileNameTask;
        private long _writeIndex;
        private string _appendFileId;

        private async Task<(long count, FileName filename)> AppendFileName(CancellationToken ct)
        {
            var files = (await _dir.ListFilesAsync(ct).ConfigureAwait(_taskHelper))
                .Select(x => new FileName(x))
                .Where(x => x.Prefix == _prefix)
                .OrderByDescending(x => x.SeqId)
                .ToList();
            return (files.Sum(x => x.ElementCount), files.FirstOrDefault());
        }

        // Not thread-safe
        public async Task<long> AppendAsync(IEnumerable<T> items, CancellationToken ct = default(CancellationToken))
        {
            var builder = ImmutableArray.CreateBuilder<byte>();
            int byteLength = -1;
            int itemCount = 0;
            foreach (var item in items)
            {
                builder.Add(ID_BYTE_1);
                builder.Add(ID_BYTE_2);
                var rawBytes = _factory.Store(item);
                builder.AddRange(rawBytes);
                builder.AddRange(FletcherChecksum.Calc16Bytes(rawBytes));
                if (byteLength == -1)
                {
                    byteLength = builder.Count;
                }
                itemCount += 1;
            }
            if (itemCount == 0)
            {
                // No items to store, so return.
                return -1;
            }
            var bytes = builder.ToImmutableArray();
            if (_appendFileNameTask != null)
            {
                var (count, fileName) = await _appendFileNameTask.ConfigureAwait(_taskHelper);
                int seqId = -1;
                if (fileName == null)
                {
                    seqId = 1;
                }
                else
                {
                    if (fileName.Version != _factory.CurrentVersion)
                    {
                        seqId = fileName.SeqId + 1;
                    }
                    else if (fileName.ElementSize != byteLength)
                    {
                        throw new InvalidOperationException("Same version, but different sizes.");
                    }
                }
                if (seqId >= 0)
                {
                    fileName = new FileName(_prefix, seqId, _factory.CurrentVersion, byteLength);
                }
                _writeIndex = count;
                _appendFileId = fileName.Id;
                _appendFileNameTask = null;
            }
            await _dir.AppendAsync(_appendFileId, bytes, ct).ConfigureAwait(_taskHelper);
            long writeIndex = _writeIndex;
            _writeIndex += itemCount;
            return writeIndex;
        }

        public Task<long> AppendAsync(T item, CancellationToken ct = default(CancellationToken)) => AppendAsync(new[] { item }, ct);
    }
}

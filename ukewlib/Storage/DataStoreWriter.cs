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
        public DataStoreWriter(ITaskHelper taskHelper, IDirectory dir)
        {
            _taskHelper = taskHelper;
            _dir = dir;
            _factory = Singletons<TFactory>.Instance;
            // Find last file, and check to see if it's version-compatible
            _appendFileNameTask = _taskHelper.Run(() => AppendFileName(CancellationToken.None));
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IDirectory _dir;
        private readonly TFactory _factory;

        private Task<FileName> _appendFileNameTask;
        private string _appendFileId;

        private async Task<FileName> AppendFileName(CancellationToken ct) =>
            (await _dir.ListFilesAsync(ct).ConfigureAwait(_taskHelper))
                .Select(x => new FileName(x))
                .OrderByDescending(x => x.SeqId)
                .FirstOrDefault();

        // Not thread-safe
        public async Task AppendAsync(IEnumerable<T> items, CancellationToken ct = default(CancellationToken))
        {
            var builder = ImmutableArray.CreateBuilder<byte>();
            int byteLength = -1;
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
            }
            if (byteLength == -1)
            {
                // No items to store, so return.
                return;
            }
            var bytes = builder.ToImmutableArray();
            if (_appendFileNameTask != null)
            {
                FileName fileName = await _appendFileNameTask.ConfigureAwait(_taskHelper);
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
                    fileName = new FileName(seqId, _factory.CurrentVersion, byteLength);
                }
                _appendFileId = fileName.Id;
                _appendFileNameTask = null;
            }
            await _dir.AppendAsync(_appendFileId, bytes, ct).ConfigureAwait(_taskHelper);
        }

        public Task AppendAsync(T item, CancellationToken ct = default(CancellationToken)) => AppendAsync(new[] { item }, ct);
    }
}

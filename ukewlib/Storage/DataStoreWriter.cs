using System;
using System.Collections.Immutable;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class DataStoreWriter<T, TFactory> : DataStore where T : IStorable<T, TFactory> where TFactory : IStorableFactory<T>, new()
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

        public async Task AppendAsync(T item, CancellationToken ct = default(CancellationToken))
        {
            var rawBytes = _factory.Store(item);
            var bytes = ImmutableArray.Create(ID_BYTE_1, ID_BYTE_2).AddRange(rawBytes).AddRange(FletcherChecksum.Calc16Bytes(rawBytes));
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
                    else if (fileName.ElementSize != bytes.Length)
                    {
                        throw new InvalidOperationException("Same version, but different sizes.");
                    }
                }
                if (seqId >= 0)
                {
                    fileName = new FileName(seqId, _factory.CurrentVersion, bytes.Length);
                }
                _appendFileId = fileName.Id;
                _appendFileNameTask = null;
            }
            await _dir.AppendAsync(_appendFileId, bytes, ct).ConfigureAwait(_taskHelper);
        }
    }
}

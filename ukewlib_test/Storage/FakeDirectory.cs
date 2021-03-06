using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class FakeDirectory : IDirectory
    {
        public FakeDirectory(ITaskHelper taskHelper, params (string, ImmutableArray<byte>)[] files)
        {
            TaskHelper = taskHelper;
            _files = files.ToDictionary(x => x.Item1, x => x.Item2);
        }

        private readonly Dictionary<string, ImmutableArray<byte>> _files;
        private readonly LinkedList<TaskCompletionSource<int>> _watchTcss = new LinkedList<TaskCompletionSource<int>>();
        private int _watchTcssEpoch = 0;

        public ITaskHelper TaskHelper { get; }

        public Task<IEnumerable<FileInfo>> ListFilesAsync(CancellationToken ct = default(CancellationToken)) =>
            Task.FromResult(_files.Select(x => new FileInfo(x.Key, x.Value.Length)));

        public Task AppendAsync(string fileId, IEnumerable<byte> bytes, CancellationToken ct = default(CancellationToken))
        {
            ImmutableArray<byte> data;
            if (!_files.TryGetValue(fileId, out data))
            {
                data = ImmutableArray<byte>.Empty;
            }
            _files[fileId] = data.AddRange(bytes);
            lock (_watchTcss)
            {
                foreach (var tcs in _watchTcss)
                {
                    tcs.SetResult(0);
                }
                _watchTcss.Clear();
                _watchTcssEpoch += 1;
            }
            return Task.CompletedTask;
        }

        public Task<Stream> ReadAsync(string fileId, CancellationToken ct = default) =>
            Task.FromResult((Stream)new MemoryStream(_files[fileId].ToArray()));

        public Task<IEnumerable<string>> ListDirectoriesAsync(CancellationToken ct = default) =>
            throw new NotImplementedException();

        public Task<IDirectory> GetDirectoryAsync(string dirId, CancellationToken ct = default) =>
            throw new NotImplementedException();

        public Task AwaitChange(string filter, CancellationToken ct)
        {
            // Ignores filter, but never mind, it's probably OK for testing.
            var tcs = new TaskCompletionSource<int>();
            lock (_watchTcss)
            {
                var node = _watchTcss.AddLast(tcs);
                var epoch = _watchTcssEpoch;
                ct.Register(() =>
                {
                    lock (_watchTcss)
                    {
                        if (epoch == _watchTcssEpoch)
                        {
                            _watchTcss.Remove(node);
                        }
                    }
                });
            }
            return tcs.Task;
        }
    }
}

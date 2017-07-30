using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Utils;

namespace Ukew.Storage
{
    public class FakeDirectory : IDirectory
    {
        public FakeDirectory(params (string, ImmutableArray<byte>)[] files)
        {
            _files = files.ToDictionary(x => x.Item1, x => x.Item2);
        }

        private readonly Dictionary<string, ImmutableArray<byte>> _files;
        private readonly LinkedList<Action> _onChange = new LinkedList<Action>();

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
            lock (_onChange)
            {
                foreach (var fn in _onChange)
                {
                    Task.Run(fn);
                }
            }
            return Task.CompletedTask;
        }

        public Task<Stream> ReadAsync(string fileId, CancellationToken ct = default(CancellationToken)) =>
            Task.FromResult((Stream)new MemoryStream(_files[fileId].ToArray()));

        public IDisposable RegisterOnChange(Action fn)
        {
            lock (_onChange)
            {
                var node = _onChange.AddLast(fn);
                return new DisposeFn(() =>
                {
                    lock (_onChange)
                    {
                        _onChange.Remove(node);
                    }
                });
            }
        }
    }
}

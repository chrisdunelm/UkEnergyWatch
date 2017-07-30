using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class SystemDirectory : IDirectory
    {
        public SystemDirectory(ITaskHelper taskHelper, string path)
        {
            _taskHelper = taskHelper;
            _path = Path.GetFullPath(path);
            if (!Directory.Exists(_path))
            {
                throw new ArgumentException($"Directory: '{_path}' does not exist, or is not a directory.");
            }
        }

        private readonly ITaskHelper _taskHelper;
        private readonly string _path;
        private readonly LinkedList<Action> _onChange = new LinkedList<Action>();
        private CancellationTokenSource _watcherCts;
        private Task _watcherTask;

        public Task<IEnumerable<FileInfo>> ListFilesAsync(CancellationToken ct) =>
            Task.FromResult(Directory.EnumerateFiles(_path).Select(x => new FileInfo(Path.GetFileName(x), new System.IO.FileInfo(x).Length)));

        public async Task AppendAsync(string fileId, IEnumerable<byte> bytes, CancellationToken ct)
        {
            var b = bytes.ToArray();
            using (var f = File.Open(Path.Combine(_path, fileId), FileMode.Append, FileAccess.Write, FileShare.Read))
            {
                await f.WriteAsync(b, 0, b.Length, ct).ConfigureAwait(_taskHelper);
                await f.FlushAsync();
            }
        }

        public Task<Stream> ReadAsync(string fileId, CancellationToken ct) =>
            Task.FromResult((Stream)File.OpenRead(Path.Combine(_path, fileId)));

        private async Task Watcher(CancellationToken ct)
        {
            var pollingInterval = Duration.FromSeconds(10);
            string FingerPrint() =>
                string.Join(":", Directory.EnumerateFiles(_path)
                    .OrderBy(x => x)
                    .Select(x => new System.IO.FileInfo(x))
                    .Select(x => $"{x.Name}/{x.LastWriteTimeUtc.Ticks}/{x.Length}"));
            var fingerPrint = FingerPrint();
            while (true)
            {
                await _taskHelper.Delay(pollingInterval, ct);
                var fingerPrint1 = FingerPrint();
                if (fingerPrint != fingerPrint1)
                {
                    _onChange.Locked(() => _onChange.ToList()).ForEach(fn => Task.Run(fn));
                }
                fingerPrint = fingerPrint1;
            }
        }

        public IDisposable RegisterOnChange(Action fn)
        {
            lock (_onChange)
            {
                if (_onChange.Count == 0)
                {
                    _watcherCts = new CancellationTokenSource();
                    Task.Run(() => Watcher(_watcherCts.Token));
                }
                var node = _onChange.AddLast(fn);
                return new DisposeFn(() =>
                {
                    lock (_onChange)
                    {
                        _onChange.Remove(node);
                        if (_onChange.Count == 0)
                        {
                            _watcherCts.Cancel();
                            _watcherCts = null;
                            _watcherTask.Wait();
                            _watcherTask = null;
                        }
                    }
                });
            }
        }
    }
}

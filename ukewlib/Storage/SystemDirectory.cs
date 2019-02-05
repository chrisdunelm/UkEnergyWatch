using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.FileProviders;
using NodaTime;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class SystemDirectory : IDirectory
    {
        public SystemDirectory(ITaskHelper taskHelper, string path)
        {
            TaskHelper = taskHelper;
            _path = Path.GetFullPath(path);
            if (!Directory.Exists(_path))
            {
                throw new ArgumentException($"Directory: '{_path}' does not exist, or is not a directory.");
            }
            _fileProvider = new PhysicalFileProvider(_path);
        }

        private readonly string _path;
        private readonly IFileProvider _fileProvider;

        public ITaskHelper TaskHelper { get; }

        public Task<IEnumerable<string>> ListDirectoriesAsync(CancellationToken ct = default)
        {
            return Task.FromResult(
                Directory.EnumerateDirectories(_path)
                    .Select(x => x.Split('/', '\\').Last()));
        }

        public Task<IDirectory> GetDirectoryAsync(string dirId, CancellationToken ct = default)
        {
            var path = Path.Combine(_path, dirId);
            var exists = Directory.Exists(path);
            return Task.FromResult<IDirectory>(exists ? new SystemDirectory(TaskHelper, path) : null);
        }

        public Task<IEnumerable<FileInfo>> ListFilesAsync(CancellationToken ct) =>
            Task.FromResult(Directory.EnumerateFiles(_path).Select(x => new FileInfo(Path.GetFileName(x), new System.IO.FileInfo(x).Length)));

        public async Task AppendAsync(string fileId, IEnumerable<byte> bytes, CancellationToken ct)
        {
            var b = bytes.ToArray();
            using (var f = File.Open(Path.Combine(_path, fileId), FileMode.Append, FileAccess.Write, FileShare.Read))
            {
                await f.WriteAsync(b, 0, b.Length, ct).ConfigureAwait(TaskHelper);
                await f.FlushAsync();
            }
        }

        public Task<Stream> ReadAsync(string fileId, CancellationToken ct)
        {
            Stream result;
            try
            {
                result = File.OpenRead(Path.Combine(_path, fileId));
            }
            catch (FileNotFoundException)
            {
                result = null;
            }
            return Task.FromResult(result);
        }

        public Task AwaitChange(string filter, CancellationToken ct = default)
        {
            var tcs = new TaskCompletionSource<int>();
            var token = _fileProvider.Watch(filter);
            token.RegisterChangeCallback(_ => tcs.TrySetResult(0), null);
            return tcs.Task;
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public interface IDirectory
    {
        ITaskHelper TaskHelper { get; }
        Task<IEnumerable<string>> ListDirectoriesAsync(CancellationToken ct = default);
        Task<IDirectory> GetDirectoryAsync(string dirId, CancellationToken ct = default);
        Task<IEnumerable<Ukew.Storage.FileInfo>> ListFilesAsync(CancellationToken ct = default);
        Task<Stream> ReadAsync(string fileId, CancellationToken ct = default);
        Task AppendAsync(string fileId, IEnumerable<byte> bytes, CancellationToken ct = default);
        Task AwaitChange(string filter, CancellationToken ct = default);
    }

    public static class IDirectoryExtensions
    {
        public static async Task<IDirectory> CdAsync(this IDirectory dir, string relativePath, CancellationToken ct = default)
        {
            if (relativePath.StartsWith("/"))
            {
                throw new ArgumentException("Absolute paths not valid.", nameof(relativePath));
            }
            var parts = relativePath.Split(new[] { '/' }, StringSplitOptions.RemoveEmptyEntries);
            foreach (var part in parts)
            {
                dir = await dir.GetDirectoryAsync(part, ct).ConfigureAwait(dir.TaskHelper);
                if (dir == null) return null;
            }
            return dir;
        }

        public static async Task<Stream> ReadPathAsync(this IDirectory dir, string relativePath, CancellationToken ct = default)
        {
            var lastSlashPos = relativePath.LastIndexOf('/');
            if (lastSlashPos < 0)
            {
                return await dir.ReadAsync(relativePath, ct).ConfigureAwait(dir.TaskHelper);
            }
            var filename = relativePath.Substring(lastSlashPos + 1);
            var path = relativePath.Substring(0, lastSlashPos);
            var fileDir = await dir.CdAsync(path, ct).ConfigureAwait(dir.TaskHelper);
            if (fileDir == null)
            {
                return null;
            }
            return await fileDir.ReadAsync(filename, ct).ConfigureAwait(dir.TaskHelper);
        }

        public static async Task<bool> AwaitChange(this IDirectory dir, string filter, Duration timeout, CancellationToken ct = default)
        {
            var th = dir.TaskHelper;
            using (var cts = CancellationTokenSource.CreateLinkedTokenSource(ct))
            {
                var timeoutTask = dir.TaskHelper.Delay(timeout, cts.Token);
                var changeTask = dir.AwaitChange(filter, cts.Token);
                var task = await th.WhenAny(new [] { timeoutTask, changeTask }).ConfigureAwait(th);
                cts.Cancel();
                ct.ThrowIfCancellationRequested();
                return task == timeoutTask;
            }
        }
    }
}

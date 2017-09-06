using System;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Logging
{
    public class RollingFileAppender : ITextAppender
    {
        // TODO: Make this roll
        // Filename format: "{baseName}.0x{sequence ID}.log"

        public RollingFileAppender(IDirectory dir, string baseName, ITaskHelper taskHelper)
        {
            _dir = dir;
            _baseName = baseName;
            _taskHelper = taskHelper;
            _findFileTask = taskHelper.Run(() => FindFileAsync(CancellationToken.None));
        }

        private readonly AsyncLock _lock = new AsyncLock();
        private readonly IDirectory _dir;
        private readonly string _baseName;
        private readonly ITaskHelper _taskHelper;

        private Task<FileInfo> _findFileTask;
        private string _currentFileId;
        private long _currentFileLength;

        private FileInfo MakeFileInfo(int seqId) => new FileInfo($"{_baseName}.{seqId:x8}.log", 0);

        private async Task<FileInfo> FindFileAsync(CancellationToken ct)
        {
            var allFiles = await _dir.ListFilesAsync(ct).ConfigureAwait(_taskHelper);
            return allFiles.Where(x => x.Id.StartsWith(_baseName)).OrderByDescending(x => x.Id).FirstOrDefault() ?? MakeFileInfo(1);
        }

        public async Task AppendLineAsync(string line, CancellationToken ct = default(CancellationToken))
        {
            using (await _lock.LockAsync(ct).ConfigureAwait(_taskHelper))
            {
                if (_findFileTask != null)
                {
                    var file = await _findFileTask.ConfigureAwait(_taskHelper);
                    _findFileTask = null;
                    _currentFileId = file.Id;
                    _currentFileLength = file.Length;
                }
                var bytes = Encoding.UTF8.GetBytes(line + Environment.NewLine);
                await _dir.AppendAsync(_currentFileId, bytes, ct);
                _currentFileLength += bytes.Length;
            }
        }
    }
}

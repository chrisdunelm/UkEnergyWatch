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
    public abstract class DataStoreReader<T, TFactory> : DataStore where T : IStorable<T, TFactory> where TFactory : IStorableFactory<T>, new()
    {
        public DataStoreReader(ITaskHelper taskHelper, IDirectory dir)
        {
            _taskHelper = taskHelper;
            _dir = dir;
        }

        private ITaskHelper _taskHelper;
        private IDirectory _dir;

        private class FileIndex
        {
            public FileIndex(FileName fileName, int fromIndex, int toIndex) =>
                (FileName, FromIndex, ToIndex) = (fileName, fromIndex, toIndex);
            public FileName FileName { get; }
            public int FromIndex { get; }
            public int ToIndex { get; }
        }

        private async Task<ImmutableList<FileIndex>> BuildIndex(CancellationToken ct)
        {
            var files = await _dir.ListFilesAsync(ct).ConfigureAwait(_taskHelper);
            return files
                .Select(x => new FileName(x))
                .OrderBy(x => x.SeqId)
                .Aggregate(ImmutableList<FileIndex>.Empty, (result, fileName) =>
                {
                    var prev = result.LastOrDefault();
                    var fromIndex0 = prev?.ToIndex ?? 0;
                    var toIndex0 = fromIndex0 + (int)(fileName.Length / fileName.ElementSize);
                    return result.Add(new FileIndex(fileName, fromIndex0, toIndex0));
                });
        }

        public async Task<long> CountAsync(CancellationToken ct = default(CancellationToken)) =>
            (await _taskHelper.ConfigureAwait(BuildIndex(ct))).LastOrDefault()?.ToIndex ?? 0;

        public async Task<IAsyncEnumerable<T>> ReadAsync(int fromIndex = 0, int toIndex = int.MaxValue, CancellationToken ct = default(CancellationToken))
        {
            var indexedFiles = await BuildIndex(ct).ConfigureAwait(_taskHelper);
            int count = indexedFiles.LastOrDefault()?.ToIndex ?? 0;
            if (count == 0)
            {
                return new Enumerable(_taskHelper, _dir, null, 0, 0);
            }
            else
            {
                int rangedFromIndex = fromIndex.InRange(0, count);
                int rangedToIndex = toIndex.InRange(0, count);
                int fromFileIndex = indexedFiles.BinarySearch(rangedFromIndex, (x, i) => i < x.FromIndex ? -1 : i >= x.ToIndex ? 1 : 0);
                return new Enumerable(_taskHelper, _dir, indexedFiles.Skip(fromFileIndex).ToImmutableArray(), rangedFromIndex, rangedToIndex);
            }
        }

        private class Enumerable : IAsyncEnumerable<T>
        {
            public Enumerable(ITaskHelper taskHelper, IDirectory dir, IReadOnlyList<FileIndex> files, int fromIndex, int toIndex) =>
                (_taskHelper, _dir, _files, _fromIndex, _toIndex) = (taskHelper, dir, files, fromIndex, toIndex);
            private readonly ITaskHelper _taskHelper;
            private readonly IDirectory _dir;
            private readonly IReadOnlyList<FileIndex> _files;
            private readonly int _fromIndex, _toIndex;
            public IAsyncEnumerator<T> GetEnumerator() => new Enumerator(_taskHelper, _dir, _files, _fromIndex, _toIndex);
        }

        private class Enumerator : IAsyncEnumerator<T>
        {
            public Enumerator(ITaskHelper taskHelper, IDirectory dir, IReadOnlyList<FileIndex> files, int fromIndex, int toIndex) =>
                (_taskHelper, _dir, _files, _index, _toIndex) = (taskHelper, dir, files, fromIndex - 1, toIndex);

            private readonly ITaskHelper _taskHelper;
            private readonly IDirectory _dir;
            private readonly IReadOnlyList<FileIndex> _files;
            private readonly int  _toIndex;
            private readonly TFactory _factory = Singletons<TFactory>.Instance;

            private int _index;
            private int _filesIndex = -1;
            private Stream _fileStream;
            private T _current;
            private byte[] _readBuffer;

            public async Task<bool> MoveNext(CancellationToken ct)
            {
                _index += 1;
                if (_index >= _toIndex)
                {
                    Dispose();
                    return false;
                }
                if (_filesIndex == -1 || _index >= _files[_filesIndex].ToIndex)
                {
                    Dispose();
                    _filesIndex += 1;
                    var file = _files[_filesIndex];
                    _fileStream = await _dir.ReadAsync(file.FileName.Id, ct).ConfigureAwait(_taskHelper);
                    _fileStream.Seek((_index - file.FromIndex) * file.FileName.ElementSize, SeekOrigin.Begin);
                    _readBuffer = new byte[file.FileName.ElementSize];
                }
                int byteCount = await _fileStream.ReadAsync(_readBuffer, 0, _readBuffer.Length).ConfigureAwait(_taskHelper);
                if (byteCount != _readBuffer.Length)
                {
                    throw new InvalidDataException("Failed to load data");
                }
                // TODO later: Recovery from corrupt data
                if (_readBuffer[0] != ID_BYTE_1 || _readBuffer[1] != ID_BYTE_2)
                {
                    throw new InvalidDataException("Invalid ID bytes");
                }
                var bytes = ImmutableArray.Create(_readBuffer, 2, byteCount - 4);
                var calcChecksum = FletcherChecksum.Calc16Bytes(bytes);
                if (calcChecksum[0] != _readBuffer[byteCount - 2] || calcChecksum[1] != _readBuffer[byteCount - 1])
                {
                    throw new InvalidDataException("Invalid data checksum");
                }
                // TODO: Check checksum
                _current = _factory.Load(_files[_filesIndex].FileName.Version, bytes);
                return true;
            }

            public T Current => _current;

            public void Dispose()
            {
                if (_fileStream != null)
                {
                    _fileStream.Dispose();
                    _fileStream = null;
                }
                _current = default(T);
                _readBuffer = null;
            }
        }
    }
}

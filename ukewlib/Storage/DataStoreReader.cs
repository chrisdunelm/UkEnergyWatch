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
    public abstract class DataStoreReader<T, TFactory> : DataStore, IReader<T>
        where T : IStorable<T, TFactory> where TFactory : IStorableFactory<T>, new()
    {
        public DataStoreReader(ITaskHelper taskHelper, IDirectory dir, string prefix)
        {
            _taskHelper = taskHelper;
            _dir = dir;
            _prefix = prefix;
        }

        private ITaskHelper _taskHelper;
        private IDirectory _dir;
        private string _prefix;

        private class FileIndex
        {
            public FileIndex(FileName fileName, int fromIndex, int toIndex) =>
                (FileName, FromIndex, ToIndex) = (fileName, fromIndex, toIndex);
            public FileName FileName { get; }
            public int FromIndex { get; } // Inclusive
            public int ToIndex { get; } // Exclusive
        }

        private async Task<ImmutableList<FileIndex>> BuildIndex(CancellationToken ct)
        {
            var files = await _dir.ListFilesAsync(ct).ConfigureAwait(_taskHelper);
            return files
                .Select(x => new FileName(x))
                .Where(x => x.Prefix == _prefix)
                .OrderBy(x => x.SeqId)
                .Aggregate(ImmutableList<FileIndex>.Empty, (result, fileName) =>
                {
                    var prev = result.LastOrDefault();
                    var fromIndex0 = prev?.ToIndex ?? 0;
                    var toIndex0 = fromIndex0 + (int)(fileName.Length / fileName.ElementSize);
                    return result.Add(new FileIndex(fileName, fromIndex0, toIndex0));
                });
        }

        public Task AwaitChange(CancellationToken ct = default) => _dir.AwaitChange("*.datastore", ct);

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
                var fileIndexes = indexedFiles.SkipWhile(x => x.ToIndex <= rangedFromIndex);
                return new Enumerable(_taskHelper, _dir, fileIndexes.ToList(), rangedFromIndex, rangedToIndex);
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

            private const int BUFFER_SIZE = 10 * 1024 * 1024;
            private readonly ITaskHelper _taskHelper;
            private readonly IDirectory _dir;
            private readonly IReadOnlyList<FileIndex> _files;
            private readonly int  _toIndex;
            private readonly TFactory _factory = Singletons<TFactory>.Instance;

            private int _index;
            private int _filesIndex = -1;
            private Stream _fileStream;
            private T _current;
            private int _elementSize;
            private byte[] _readBuffer;
            private int _readBufferIndex;
            private int _readBufferEndIndex;
            private int _version;

            public async Task<bool> MoveNext(CancellationToken ct)
            {
                _index += 1;
                if (_index >= _toIndex)
                {
                    Dispose();
                    return false;
                }
                if (_readBufferIndex >= _readBufferEndIndex)
                {
                    // TODO: Buffer-ahead the file, concurrently with reading.
                    if (_filesIndex == -1 || _index >= _files[_filesIndex].ToIndex)
                    {
                        Dispose();
                        _filesIndex += 1;
                        var file = _files[_filesIndex];
                        _elementSize = file.FileName.ElementSize;
                        _fileStream = await _dir.ReadAsync(file.FileName.Id, ct).ConfigureAwait(_taskHelper);
                        _fileStream.Seek((_index - file.FromIndex) * _elementSize, SeekOrigin.Begin);
                        int bufferCount = BUFFER_SIZE / _elementSize;
                        int bufferSize = Math.Min(bufferCount, file.ToIndex - file.FromIndex) * _elementSize;
                        _readBuffer = new byte[bufferSize];
                        _version = file.FileName.Version;
                    }
                    int toIndex = _files[_filesIndex].ToIndex;
                    int readCount = Math.Min((toIndex - _index) * _elementSize, _readBuffer.Length);
                    int byteCount = await _fileStream.ReadAsync(_readBuffer, 0, readCount).ConfigureAwait(_taskHelper);
                    if (byteCount != readCount)
                    {
                        throw new InvalidDataException("Failed to load data");
                    }
                    _readBufferIndex = 0;
                    _readBufferEndIndex = readCount;
                }
                _current = Load();
                _readBufferIndex += _elementSize;
                T Load()
                {
                    // Local sync function, so we can use Span<T> in an async method.
                    var span = new ReadOnlySpan<byte>(_readBuffer, _readBufferIndex, _elementSize);
                    // TODO later: Recovery from corrupt data
                    if (span[0] != ID_BYTE_1 || span[1] != ID_BYTE_2)
                    {
                        throw new InvalidDataException("Invalid ID bytes");
                    }
                    var dataSpan = span.Slice(2, _elementSize - 4);
                    var calcChecksum = FletcherChecksum.Calc16Bytes(dataSpan);
                    if (calcChecksum[0] != span[_elementSize - 2] || calcChecksum[1] != span[_elementSize - 1])
                    {
                        throw new InvalidDataException("Invalid data checksum");
                    }
                    return _factory.Load(_version, dataSpan);
                }
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

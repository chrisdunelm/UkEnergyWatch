using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Storage;
using Ukew.Utils.Tasks;

namespace Ukew.MemDb
{
    public class Db<T> : IDisposable where T : struct
    {
        public Db(ITaskHelper taskHelper, IReader<T> reader, int blockSize = 0, Duration? pollInterval = null, Duration? maxJitter = null)
        {
            if (blockSize == 0)
            {
                int tSize = System.Runtime.InteropServices.Marshal.SizeOf<T>();
                blockSize = 2_000_000 / tSize; // Make each block take ~2MB
            }
            _taskHelper = taskHelper;
            _reader = reader;
            // Minimum block size is 2^6 = 64 items
            for (_blockPower = 6; ; _blockPower += 1)
            {
                _blockSize = 1 << _blockPower;
                if (_blockSize >= blockSize)
                {
                    break;
                }
            }
            _blockMask = _blockSize - 1;
            _pollInterval = pollInterval ?? Duration.FromMinutes(15);
            _maxJitter = maxJitter ?? _pollInterval / 4;
            _cts = new CancellationTokenSource();
            _tcs = new TaskCompletionSource<int>();
            _rnd = new Random();
            taskHelper.Run(ReadAsync);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IReader<T> _reader;
        private readonly int _blockPower;
        private readonly int _blockSize;
        private readonly int _blockMask;
        private readonly Duration _pollInterval;
        private readonly Duration _maxJitter;
        private readonly CancellationTokenSource _cts;
        private readonly TaskCompletionSource<int> _tcs;
        private readonly Random _rnd;

        private List<T[]> _blocks = new List<T[]>();
        private T[] _lastData;
        private int _count = 0;

        public Task InitialiseTask => _tcs.Task;

        private void Add(T item)
        {
            var index = _count & _blockMask;
            if (index == 0)
            {
                _lastData = new T[_blockSize];
                _blocks.Add(_lastData);
            }
            _lastData[index] = item;
            _count += 1;
        }

        private async Task ReadAsync()
        {
            bool first = true;
            while (true)
            {
                // Read as much as is available
                var en = (await _reader.ReadAsync(_count, ct: _cts.Token).ConfigureAwait(_taskHelper)).GetEnumerator();
                while (await en.MoveNext(_cts.Token).ConfigureAwait(_taskHelper))
                {
                    Add(en.Current);
                }
                if (first)
                {
                    _tcs.SetResult(0);
                    first = false;
                }
                // Schedule next read
                var ofs = Duration.FromSeconds((_rnd.NextDouble() - 0.5) * _maxJitter.TotalSeconds);
                await _taskHelper.Delay(_pollInterval + ofs).ConfigureAwait(_taskHelper);
            }
        }

        public ImmutableArray<T> Where(Func<T, bool> predicate)
        {
            var count = _count;
            var result = ImmutableArray.CreateBuilder<T>(_blockSize);
            foreach (var block in _blocks)
            {
                foreach (var item in block)
                {
                    if (count == 0)
                    {
                        return result.ToImmutable();
                    }
                    count -= 1;
                    if (predicate(item))
                    {
                        result.Add(item);
                    }
                }
            }
            throw new InvalidOperationException("Should not ever get here");
        }

        public ImmutableArray<TResult> WhereSelect<TResult>(Func<T, TResult?> predicateProjection) where TResult : struct
        {
            var count = _count;
            var result = ImmutableArray.CreateBuilder<TResult>(_blockSize);
            foreach (var block in _blocks)
            {
                foreach (var item0 in block)
                {
                    if (count == 0)
                    {
                        return result.ToImmutable();
                    }
                    count -= 1;
                    var item1 = predicateProjection(item0);
                    if (item1 != null)
                    {
                        result.Add(item1.Value);
                    }
                }
            }
            throw new InvalidOperationException("Should not ever get here");
        }

        public void Dispose()
        {
            _cts.Cancel();
        }
    }
}

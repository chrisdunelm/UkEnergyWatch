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
    public class Db<T> : DbReader<T>, IDisposable where T : struct
    {
        public Db(ITaskHelper taskHelper, IReader<T> reader, int requestedBlockSize = 0, Duration? pollInterval = null, Duration? maxJitter = null)
            : base(requestedBlockSize)
        {
            _taskHelper = taskHelper;
            _reader = reader;
            _pollInterval = pollInterval ?? Duration.FromMinutes(15);
            _maxJitter = maxJitter ?? _pollInterval / 4;
            _cts = new CancellationTokenSource();
            _tcs = new TaskCompletionSource<int>();
            _rnd = new Random();
            // Start load of initial data
            taskHelper.Run(ReadAsync);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IReader<T> _reader;
        private readonly Duration _pollInterval;
        private readonly Duration _maxJitter;
        private readonly CancellationTokenSource _cts;
        private readonly TaskCompletionSource<int> _tcs;
        private readonly Random _rnd;

        public Task InitialiseTask => _tcs.Task;

        private async Task ReadAsync()
        {
            bool first = true;
            while (true)
            {
                // Read as much as is available
                var en = (await _reader.ReadAsync(Count, ct: _cts.Token).ConfigureAwait(_taskHelper)).GetEnumerator();
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

        public void Dispose()
        {
            _cts.Cancel();
        }

        /*public ImmutableArray<T> Where(Func<T, bool> predicate)
        {
            var count = _count;
            var result = ImmutableArray.CreateBuilder<T>(_blockSize);
            foreach (var block in _blocks)
            {
                var length = Math.Min(count, block.Length);
                for (int i = 0; i < length; i += 1)
                {
                    if (predicate(block[i]))
                    {
                        result.Add(block[i]);
                    }
                }
                count -= length;
            }
            if (count != 0)
            {
                throw new InvalidOperationException("count != 0. Bug somewhere!");
            }
            return result.ToImmutable();
        }

        public ImmutableArray<TResult> WhereSelect<TResult>(Func<T, TResult?> predicateProjection) where TResult : struct
        {
            var count = _count;
            var result = ImmutableArray.CreateBuilder<TResult>(_blockSize);
            foreach (var block in _blocks)
            {
                var length = Math.Min(count, block.Length);
                for (int i = 0; i < length; i += 1)
                {
                    var projN = predicateProjection(block[i]);
                    if (projN is TResult proj)
                    {
                        result.Add(proj);
                    }
                }
                count -= length;
            }
            if (count != 0)
            {
                throw new InvalidOperationException("count != 0. Bug somewhere!");
            }
            return result.ToImmutable();
        }*/

    }
}

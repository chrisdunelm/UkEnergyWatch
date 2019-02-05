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
        public Db(ITaskHelper taskHelper, IReader<T> reader, int requestedBlockSize = 0, Duration? pollInterval = null, Duration? maxJitter = null, bool disableWatch = false)
            : base(requestedBlockSize)
        {
            _taskHelper = taskHelper;
            _reader = reader;
            _pollInterval = pollInterval ?? Duration.FromMinutes(15);
            _maxJitter = maxJitter ?? (_pollInterval / 4);
            _enableWatch = !disableWatch;
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
        private readonly bool _enableWatch;
        private readonly CancellationTokenSource _cts;
        private readonly TaskCompletionSource<int> _tcs;
        private readonly Random _rnd;

        public Task InitialiseTask => _tcs.Task;

        public Db<T> WaitUntilInitialised()
        {
            InitialiseTask.Wait(_taskHelper);
            return this;
        }

        private async Task ReadAsync()
        {
            var ct = _cts.Token;
            await ReadAvailableAsync(ct).ConfigureAwait(_taskHelper);
            _tcs.SetResult(0);
            while (true)
            {
                // Start watch now, so no watch events are missed
                var delay = _pollInterval + _maxJitter * (_rnd.NextDouble() - 0.5);
                Task watchTask = _enableWatch ? _reader.AwaitChange(delay, ct) : _taskHelper.Delay(delay, ct);
                // Read as much as is available
                await ReadAvailableAsync(ct).ConfigureAwait(_taskHelper);
                // Schedule/wait-for next read
                await watchTask.ConfigureAwait(_taskHelper);
            }
            async Task ReadAvailableAsync(CancellationToken ct0)
            {
                var en = (await _reader.ReadAsync((int)Count, ct: ct0).ConfigureAwait(_taskHelper)).GetEnumerator();
                while (await en.MoveNext(ct0).ConfigureAwait(_taskHelper))
                {
                    Add(en.Current);
                }
            }
        }

        public void Dispose()
        {
            _cts.Cancel();
        }
    }
}

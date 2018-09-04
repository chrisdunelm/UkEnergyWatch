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
            bool first = true;
            while (true)
            {
                using (var combCts = CancellationTokenSource.CreateLinkedTokenSource(_cts.Token))
                {
                    // Start watch now, so no watch events are missed
                    Task watchTask = _enableWatch ? _reader.AwaitChange(combCts.Token) : null;
                    // Read as much as is available
                    var en = (await _reader.ReadAsync((int)Count, ct: _cts.Token).ConfigureAwait(_taskHelper)).GetEnumerator();
                    while (await en.MoveNext(_cts.Token).ConfigureAwait(_taskHelper))
                    {
                        Add(en.Current);
                    }
                    if (first)
                    {
                        _tcs.SetResult(0);
                        first = false;
                    }
                    // Schedule/wait-for next read
                    var ofs = Duration.FromSeconds((_rnd.NextDouble() - 0.5) * _maxJitter.TotalSeconds);
                    var delay = _pollInterval + ofs;
                    Task delayTask = _taskHelper.Delay(delay, combCts.Token);
                    var tasks = watchTask == null ? new[] { delayTask } : new[] { delayTask, watchTask };
                    await _taskHelper.WhenAny(tasks).ConfigureAwait(_taskHelper);
                    combCts.Cancel();
                    await _taskHelper.WhenAll(tasks).ConfigureAwaitHideCancel(_taskHelper);
                }
            }
        }

        public void Dispose()
        {
            _cts.Cancel();
        }
    }
}

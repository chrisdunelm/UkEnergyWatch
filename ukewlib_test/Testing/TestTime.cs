using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.ExceptionServices;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using NodaTime.Testing;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Testing
{
    public class TestTime : ITime, IDisposable
    {
        public sealed class SchedulerException : Exception
        {
            public SchedulerException(string message) : base(message) { }
        }

        public abstract class SimpleThreadPool
        {
            // Task completes successfully when Thread action has finished (normal exit or exception)
            public virtual Task Start(Action action) => throw new NotImplementedException();
        }

        public class DefaultSimpleThreadPool : SimpleThreadPool
        {
            public static DefaultSimpleThreadPool Instance { get; } = new DefaultSimpleThreadPool();

            private DefaultSimpleThreadPool() { }

            public override Task Start(Action action)
            {
                var threadTcs = new TaskCompletionSource<int>();
                void ActionWrapper()
                {
                    try
                    {
                        action();
                    }
                    finally
                    {
                        threadTcs.SetResult(0);
                    }
                }
                var thread = new Thread(ActionWrapper);
                thread.IsBackground = true;
                thread.Start();
                return threadTcs.Task;
            }
        }

        public class CachingSimpleThreadPool : SimpleThreadPool
        {
            public static CachingSimpleThreadPool Instance { get; } = new CachingSimpleThreadPool();

            private class ThreadRunner : IDisposable
            {
                public ThreadRunner()
                {
                    _thread = new Thread(ThreadFn);
                    _thread.IsBackground = true;
                    _thread.Start();
                }

                private Thread _thread;
                private Queue<Action> _actions = new Queue<Action>();
                private volatile bool _disposed;
                private volatile TaskCompletionSource<int> _signal = new TaskCompletionSource<int>();

                private void ThreadFn()
                {
                    while (!_disposed)
                    {
                        _signal.Task.Wait();
                        _signal = new TaskCompletionSource<int>();
                        _actions.Locked(() => _actions.Count > 0 ? _actions.Dequeue() : null)?.Invoke();
                    }
                }

                public void Run(Action action)
                {
                    Extensions.Locked(_actions, () => _actions.Enqueue(action));
                    _actions.Locked(() => _actions.Enqueue(action));
                    _signal.TrySetResult(0);
                }

                public void Dispose()
                {
                    _disposed = true;
                    _signal.TrySetResult(0);
                }
            }

            private const int CacheSize = 30;

            private CachingSimpleThreadPool() { }

            private Stack<ThreadRunner> _cache = new Stack<ThreadRunner>();

            public override Task Start(Action action)
            {
                var runner = _cache.Locked(() => _cache.Count == 0 ? new ThreadRunner() : _cache.Pop());
                var threadTcs = new TaskCompletionSource<int>();
                void ActionWrapper()
                {
                    try
                    {
                        action();
                    }
                    finally
                    {
                        lock (_cache)
                        {
                            if (_cache.Count >= CacheSize)
                            {
                                runner.Dispose();
                            }
                            else
                            {
                                _cache.Push(runner);
                            }
                        }
                        threadTcs.SetResult(0);
                    }
                }
                runner.Run(ActionWrapper);
                return threadTcs.Task;
            }
        }

        private class TestTaskScheduler : TaskScheduler, IDisposable
        {
            public TestTaskScheduler(int threadCount, SimpleThreadPool threadPool = null)
            {
                threadPool = threadPool ?? CachingSimpleThreadPool.Instance;
                MaximumConcurrencyLevel = threadCount;
                lock (_lock)
                {
                    _activeThreadCount = threadCount;
                }
                _events = Enumerable.Range(0, threadCount).Select(_ => new AutoResetEvent(false)).ToArray();
                _runEvent = new AutoResetEvent(false);
                _threads = Enumerable.Range(0, threadCount).Select(i => threadPool.Start(() => RunThread(_events[i]))).ToArray();
            }

            private readonly object _lock = new object();
            private readonly AutoResetEvent[] _events;
            private readonly Task[] _threads;
            private readonly AutoResetEvent _runEvent;

            private readonly Queue<Task> _taskQueue = new Queue<Task>();
            
            // Key: Task that is waiting; Value: task that is being waited on
            private readonly Dictionary<Task, Task> _waitingTasks = new Dictionary<Task, Task>();

            private int _activeThreadCount;
            private bool _running;
            private CancellationTokenSource _disposedCts = new CancellationTokenSource();

            [ThreadStatic]
            private static Task t_currentTask;

            public override int MaximumConcurrencyLevel { get; }

            private void RunThread(AutoResetEvent ev)
            {
                while (true)
                {
                    t_currentTask = null;
                    lock (_lock)
                    {
                        _activeThreadCount -= 1;
                    }
                    while (true)
                    {
                        if (_disposedCts.IsCancellationRequested)
                        {
                            return;
                        }
                        lock (_lock)
                        {
                            if (_taskQueue.Count > 0 && _running)
                            {
                                t_currentTask = _taskQueue.Dequeue();
                                _activeThreadCount += 1;
                                break;
                            }
                        }
                        _runEvent.Set();
                        ev.WaitOne();
                    }
                    TryExecuteTask(t_currentTask);
                }
            }

            protected override IEnumerable<Task> GetScheduledTasks()
            {
                lock (_lock)
                {
                    return _taskQueue.ToArray();
                }
            }

            private void SetAllEvents()
            {
                foreach (var ev in _events)
                {
                    ev.Set();
                }
            }

            protected override void QueueTask(Task task)
            {
                lock (_lock)
                {
                    _taskQueue.Enqueue(task);
                }
                SetAllEvents();
            }

            protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
            {
                lock (_lock)
                {
                    _taskQueue.Enqueue(task);
                }
                SetAllEvents();
                return false;
            }

            public void Wait(Task task)
            {
                // Task is in a blocking wait. Track tasks being blocked on.
                lock (_lock)
                {
                    _waitingTasks.Add(t_currentTask, task);
                }
                _runEvent.Set();
                try
                {
                    task.Wait(_disposedCts.Token);
                }
                finally
                {
                    lock (_lock)
                    {
                        _waitingTasks.Remove(t_currentTask);
                    }
                }
            }

            public bool RunUntilIdle(CancellationToken ct)
            {
                lock (_lock)
                {
                    _running = true;
                }
                SetAllEvents(); // Get threads started
                while (true)
                {
                    bool moreTodo;
                    lock (_lock)
                    {
                        moreTodo = _taskQueue.Count + _activeThreadCount - _waitingTasks.Count > 0 || _waitingTasks.Values.Any(x => x.IsCompleted);
                    }
                    if (!moreTodo)
                    {
                        lock (_lock)
                        {
                            _running = false;
                        }
                        return true; // All Tasks run, now idle
                    }
                    WaitHandle.WaitAny(new[] { ct.WaitHandle, _runEvent });
                    lock (_lock)
                    {
                        if (ct.IsCancellationRequested)
                        {
                            _taskQueue.Clear();
                            _running = false;
                            SetAllEvents();
                            return false;
                        }
                        if (_waitingTasks.Count == _threads.Length)
                        {
                            _running = false;
                            throw new SchedulerException($"All {_threads.Length} threads blocking. This code requires more threads in the thread-pool.");
                        }
                    }
                }
            }

            public void Dispose()
            {
                _disposedCts.Cancel();
                SetAllEvents();
                Task.WaitAll(_threads);
            }
        }


        private class TestTaskHelper : ITaskHelper
        {
            public TestTaskHelper(TestTime time)
            {
                _time = time;
            }

            private readonly TestTime _time;

            public TaskScheduler TaskScheduler => _time._taskScheduler;

            public async Task<T> Run<T>(Func<Task<T>> function) =>
                await ConfigureAwait(await ConfigureAwait(Task<Task<T>>.Factory.StartNew(function, CancellationToken.None, TaskCreationOptions.None, TaskScheduler)));

            public Task Delay(Duration Duration, CancellationToken ct = default(CancellationToken)) => _time.Delay(Duration, ct);

            public void Wait(Task task)
            {
                Interlocked.Increment(ref _time._waitCount);
                _time._taskScheduler.Wait(task);
            }

            public TaskAwaitable ConfigureAwait(Task task) =>
                new TaskAwaitable(new TestAwaiter(task, TaskScheduler));

            public TaskAwaitable<T> ConfigureAwait<T>(Task<T> task) =>
                new TaskAwaitable<T>(new TestAwaiter<T>(task, TaskScheduler));

            public async Task WhenAll(IEnumerable<Task> tasks)
            {
                var exceptions = new List<Exception>();
                var cancelled = false;
                foreach (var task in tasks)
                {
                    await this.ConfigureAwaitHideErrors(task);
                    switch (task.Status)
                    {
                        case TaskStatus.RanToCompletion:
                            break;
                        case TaskStatus.Faulted:
                            exceptions.AddRange(task.Exception.InnerExceptions);
                            break;
                        case TaskStatus.Canceled:
                            cancelled = true;
                            break;
                        default:
                            throw new InvalidOperationException($"Impossible Task status: {task.Status}");
                    }
                }
                if (exceptions.Count > 0)
                {
                    throw new AggregateException(exceptions);
                }
                if (cancelled)
                {
                    throw new TaskCanceledException();
                }
            }

            public Task<Task> WhenAny(IEnumerable<Task> tasks)
            {
                var tcs = new TaskCompletionSource<Task>();
                foreach (var task in tasks)
                {
                    this.Run(async () =>
                    {
                        await this.ConfigureAwaitHideErrors(task);
                        tcs.TrySetResult(task);
                    });
                }
                return tcs.Task;
            }
        }

        private struct DelayTask
        {
            public DelayTask(Instant scheduled, CancellationToken ct)
            {
                Scheduled = scheduled;
                CancellationToken = ct;
                Tcs = new TaskCompletionSource<int>();
            }
            public Instant Scheduled { get; }
            public CancellationToken CancellationToken { get; }
            public TaskCompletionSource<int> Tcs { get; }
        }

        public TestTime(int threadCount = 1)
        {
            _fakeClock = new FakeClock(Instant.FromUtc(2000, 1, 1, 0, 0));
            _taskScheduler = new TestTaskScheduler(threadCount);
            TaskHelper = new TestTaskHelper(this);
        }

        private readonly object _lock = new object();
        private readonly FakeClock _fakeClock;
        private readonly TestTaskScheduler _taskScheduler;
        private readonly LinkedList<DelayTask> _delays = new LinkedList<DelayTask>();

        private int _waitCount;

        public ITaskHelper TaskHelper { get; }

        public Instant GetCurrentInstant() => _fakeClock.GetCurrentInstant();

        public int WaitCount => Interlocked.Add(ref _waitCount, 0);

        private Task Delay(Duration delay, CancellationToken cancellationToken = default(CancellationToken))
        {
            lock (_lock)
            {
                var delayTask = new DelayTask(_fakeClock.GetCurrentInstant() + delay, cancellationToken);
                var delayNode = _delays.First;
                while (delayNode != null && delayNode.Value.Scheduled < delayTask.Scheduled)
                {
                    delayNode = delayNode.Next;
                }
                if (delayNode == null)
                {
                    _delays.AddLast(delayTask);
                }
                else
                {
                    _delays.AddBefore(delayNode, delayTask);
                }
                return delayTask.Tcs.Task;
            }
        }

        public void Run(Action action) => Run(() =>
        {
            action();
            return Task.FromResult(0);
        });

        public void Run(Func<Task> taskProvider) => Run(async () =>
        {
            await TaskHelper.ConfigureAwait(taskProvider());
            return 0;
        });

        public T Run<T>(Func<Task<T>> taskProvider)
        {
            var simulatedTimeout = GetCurrentInstant() + Duration.FromHours(24);
            var realCts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
            Task<Task<T>> mainTask = Task<Task<T>>.Factory.StartNew(taskProvider, CancellationToken.None, TaskCreationOptions.None, _taskScheduler);
            while (true)
            {
                // Run all tasks
                bool ranAll = _taskScheduler.RunUntilIdle(realCts.Token);
                if (!ranAll)
                {
                    throw new SchedulerException("Real time has reached timeout. Probably caused by recusive task creation.");
                }
                if (mainTask.IsCompleted)
                {
                    if (mainTask.Exception != null)
                    {
                        throw mainTask.Exception;
                    }
                    if (mainTask.Result.IsCompleted)
                    {
                        if (mainTask.Result.Exception != null)
                        {
                            throw mainTask.Result.Exception;
                        }
                        return mainTask.Result.Result;
                    }
                }
                // Cancel Tasks, or move to next clock time
                var tasksToComplete = new List<TaskCompletionSource<int>>();
                lock (_lock)
                {
                    if (_delays.Count == 0)
                    {
                        throw new SchedulerException("Inconsistent state, delay queue should have content. This is probably caused by a misconfigured await.");
                    }
                    bool anyCancelled = false;
                    var node = _delays.First;
                    while (node != null)
                    {
                        var next = node.Next;
                        if (node.Value.CancellationToken.IsCancellationRequested)
                        {
                            node.Value.Tcs.SetCanceled();
                            _delays.Remove(node);
                            anyCancelled = true;
                        }
                        node = next;
                    }
                    if (!anyCancelled)
                    {
                        var delayTask = _delays.First.Value;
                        while (_delays.Count > 0 && _delays.First.Value.Scheduled <= delayTask.Scheduled)
                        {
                            tasksToComplete.Add(_delays.First.Value.Tcs);
                            _delays.RemoveFirst();
                        }
                        _fakeClock.Reset(delayTask.Scheduled);
                        if (GetCurrentInstant() > simulatedTimeout)
                        {
                            throw new SchedulerException("Simulated time has reached timeout.");
                        }
                    }
                }
                // Results must be set after the clock has changed, and outside the lock
                foreach (var tcs in tasksToComplete)
                {
                    tcs.SetResult(0);
                }
            }
        }

        public void Dispose()
        {
            _taskScheduler.Dispose();
        }
    }
}
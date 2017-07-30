using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;

namespace Ukew.Utils.Tasks
{
    public class SystemTaskHelper : ITaskHelper
    {
        public static SystemTaskHelper Instance { get; } = new SystemTaskHelper();

        private SystemTaskHelper() { }

        public TaskScheduler TaskScheduler => TaskScheduler.Default;
        public Task<T> Run<T>(Func<Task<T>> function) => Task.Run(function);
        public Task Delay(Duration duration, CancellationToken ct = default(CancellationToken)) => Task.Delay(duration.ToTimeSpan(), ct);
        public void Wait(Task task) => task.Wait();
        public TaskAwaitable ConfigureAwait(Task task) => new TaskAwaitable(new ForwardingAwaiter(task.ConfigureAwait(false).GetAwaiter()));
        public TaskAwaitable<T> ConfigureAwait<T>(Task<T> task) => new TaskAwaitable<T>(new ForwardingAwaiter<T>(task.ConfigureAwait(false).GetAwaiter()));
        public Task WhenAll(IEnumerable<Task> tasks) => Task.WhenAll(tasks);
        public Task<Task> WhenAny(IEnumerable<Task> tasks) => Task.WhenAny(tasks);
    }
}

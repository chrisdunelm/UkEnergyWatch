using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;

namespace Ukew.Utils.Tasks
{
    public interface ITaskHelper
    {
        TaskScheduler TaskScheduler { get; }
        Task<T> Run<T>(Func<Task<T>> function);
        Task Delay(Duration duration, CancellationToken ct = default(CancellationToken));
        void Wait(Task task);
        TaskAwaitable ConfigureAwait(Task task);
        TaskAwaitable<T> ConfigureAwait<T>(Task<T> task);
        Task WhenAll(IEnumerable<Task> tasks);
        Task<Task> WhenAny(IEnumerable<Task> tasks);
    }

    public static class ITaskHelperExtensions
    {
        public static Task Run(this ITaskHelper taskHelper, Func<Task> function) => taskHelper.Run(async () =>
        {
            await taskHelper.ConfigureAwait(function());
            return 0;
        });

        public static TaskAwaitable ConfigureAwait(this Task task, ITaskHelper taskHelper) => taskHelper.ConfigureAwait(task);

        public static TaskAwaitable<bool> ConfigureAwaitHideCancel(this Task task, ITaskHelper taskHelper)
        {
            async Task<bool> Inner()
            {
                try
                {
                    await task.ConfigureAwait(taskHelper);
                    return false;
                }
                catch (OperationCanceledException)
                {
                    return true;
                }
            }
            return Inner().ConfigureAwait(taskHelper);
        }

        public static TaskAwaitable<T> ConfigureAwait<T>(this Task<T> task, ITaskHelper taskHelper) => taskHelper.ConfigureAwait(task);

        public static void Wait(this Task task, ITaskHelper taskHelper) => taskHelper.Wait(task);

        public static TaskAwaitable<Exception> ConfigureAwaitHideErrors(this ITaskHelper taskHelper, Func<Task> task)
        {
            async Task<Exception> Inner()
            {
                try
                {
                    await taskHelper.ConfigureAwait(task());
                    return null;
                }
                catch (Exception e)
                {
                    return e;
                }
            }
            return taskHelper.ConfigureAwait(Inner());
        }

        public static TaskAwaitable<Exception> ConfigureAwaitHideErrors(this ITaskHelper taskHelper, Task task) =>
            taskHelper.ConfigureAwaitHideErrors(() => task);

        public static TaskAwaitable<T> ConfigureAwaitHideErrors<T>(this ITaskHelper taskHelper, Task<T> task, T resultOnError)
        {
            async Task<T> Inner()
            {
                try
                {
                    return await taskHelper.ConfigureAwait(task);
                }
                catch
                {
                    return resultOnError;
                }
            }
            return taskHelper.ConfigureAwait(Inner());
        }
    }
}

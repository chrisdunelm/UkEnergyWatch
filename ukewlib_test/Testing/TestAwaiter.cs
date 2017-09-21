using System;
using System.Linq;
using System.Runtime.ExceptionServices;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Utils.Tasks;

namespace Ukew.Testing
{
    public class TestAwaiter : ITaskAwaiter
    {
        public TestAwaiter(Task task, TaskScheduler taskScheduler)
        {
            _task = task;
            _taskScheduler = taskScheduler;
        }

        private Task _task;
        private TaskScheduler _taskScheduler;

        public void OnCompleted(Action continuation) =>
            _task.ContinueWith(_ => continuation(), CancellationToken.None, TaskContinuationOptions.DenyChildAttach, _taskScheduler);
        public void UnsafeOnCompleted(Action continuation) =>
            _task.ContinueWith(_ => continuation(), CancellationToken.None, TaskContinuationOptions.DenyChildAttach, _taskScheduler);
        public bool IsCompleted => _task.IsCompleted;
        public void GetResult()
        {
            try
            {
                _task.Wait();
            }
            catch (AggregateException e)
            {
                ExceptionDispatchInfo.Capture(e.InnerExceptions.FirstOrDefault() ?? e).Throw();
            }
        }
    }

    public class TestAwaiter<T> : ITaskAwaiter<T>
    {
        public TestAwaiter(Task<T> task, TaskScheduler taskScheduler)
        {
            _task = task;
            _taskScheduler = taskScheduler;
        }

        private Task<T> _task;
        private TaskScheduler _taskScheduler;

        public void OnCompleted(Action continuation) =>
            _task.ContinueWith(_ => continuation(), CancellationToken.None, TaskContinuationOptions.DenyChildAttach, _taskScheduler);
        public void UnsafeOnCompleted(Action continuation) =>
            _task.ContinueWith(_ => continuation(), CancellationToken.None, TaskContinuationOptions.DenyChildAttach, _taskScheduler);
        public bool IsCompleted => _task.IsCompleted;
        public T GetResult()
        {
            //return _task.Result;
            try
            {
                return _task.Result;
            }
            catch (AggregateException e)
            {
                ExceptionDispatchInfo.Capture(e.InnerExceptions.FirstOrDefault() ?? e).Throw();
                throw; // Will never get here, but compiler needs it.
            }
        }
    }
}

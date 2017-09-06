using System;
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
            //_task.Wait();
            try
            {
                _task.Wait();
            }
            catch (AggregateException e)
            {
                throw e.InnerException ?? e;
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
                throw e.InnerException ?? e;
            }
        }
    }
}

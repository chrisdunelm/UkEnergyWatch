namespace Ukew.Utils.Tasks
{
    public struct TaskAwaitable
    {
        public TaskAwaitable(ITaskAwaiter awaiter)
        {
            _awaiter = awaiter;
        }

        private ITaskAwaiter _awaiter;

        public ITaskAwaiter GetAwaiter() => _awaiter;
    }

    public struct TaskAwaitable<T>
    {
        public TaskAwaitable(ITaskAwaiter<T> awaiter)
        {
            _awaiter = awaiter;
        }

        private ITaskAwaiter<T> _awaiter;

        public ITaskAwaiter<T> GetAwaiter() => _awaiter;
    }
}

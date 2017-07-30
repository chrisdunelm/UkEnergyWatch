using System.Runtime.CompilerServices;

namespace Ukew.Utils.Tasks
{
    public interface ITaskAwaiter : ICriticalNotifyCompletion
    {
        bool IsCompleted { get; }
        void GetResult();
    }

    public interface ITaskAwaiter<T> : ICriticalNotifyCompletion
    {
        bool IsCompleted { get; }
        T GetResult();
    }
}

using System;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Testing
{
    public static class TimeRunner
    {
        public static void Run(Func<ITime, ITaskHelper, Task> test, int threadCount = 1, Instant? startInstant = null)
        {
            var time = new TestTime(threadCount, startInstant);
            time.Run(() => test(time, time.TaskHelper));
        }

        public static T Run<T>(Func<ITime, ITaskHelper, Task<T>> test, int threadCount = 1, Instant? startInstant = null)
        {
            var time = new TestTime(threadCount, startInstant);
            return time.Run(() => test(time, time.TaskHelper));
        }
    }
}

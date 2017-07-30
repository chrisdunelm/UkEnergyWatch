using System;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Utils.Tasks;

namespace Ukew.Utils
{
    public class Scheduler
    {
        internal static Duration NextDelay(Duration interval, Duration offset, Instant now) =>
            interval - Duration.FromMilliseconds((now.ToUnixTimeMilliseconds() - (long)offset.TotalMilliseconds) % (long)interval.TotalMilliseconds);

        public Scheduler(ITime time, ITaskHelper taskHelper)
        {
            _time = time;
            _taskHelper = taskHelper;
        }

        private readonly ITime _time;
        private readonly ITaskHelper _taskHelper;

        public Task ScheduleOne(Duration interval, Duration offset, CancellationToken ct = default(CancellationToken)) =>
            _taskHelper.Delay(NextDelay(interval, offset, _time.GetCurrentInstant()), ct);
    }
}

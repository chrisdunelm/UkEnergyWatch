using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Applications
{
    public class FetchB1610
    {
        public FetchB1610(ITaskHelper taskHelper, IElexonDownloader downloader, IDirectory dir, ITime time)
        {
            _taskHelper = taskHelper;
            _time = time;
            _scheduler = new Scheduler(time, taskHelper);
            _b1610 = new B1610(taskHelper, downloader);
            _writer = new B1610.Writer(taskHelper, dir);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly ITime _time;
        private readonly Scheduler _scheduler;
        private readonly B1610 _b1610;
        private readonly B1610.Writer _writer;

        public async Task Start(bool startImmediately, CancellationToken ct = default(CancellationToken))
        {
            bool wait = !startImmediately;
            while (true)
            {
                if (wait)
                {
                    await _scheduler.ScheduleOne(Duration.FromMinutes(30), Duration.FromMinutes(13), ct).ConfigureAwait(_taskHelper);
                }
                wait = true;
                // Fetch the settlement data/period just over 1 week ago.
                // I'm not entirely sure when the data is released. It's definitely roughly 1 week behind.
                Instant fetchTime = _time.GetCurrentInstant() - Duration.FromDays(7) - Duration.FromMinutes(30);
                var data = await _b1610.GetAsync(fetchTime.SettlementDate(), fetchTime.SettlementPeriod(), ct);
                await _writer.AppendAsync(data, ct);
            }
        }
    }
}

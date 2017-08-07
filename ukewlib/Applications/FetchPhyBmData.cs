using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Applications
{
    public class FetchPhyBmData
    {
        public FetchPhyBmData(ITaskHelper taskHelper, IElexonDownloader downloader, IDirectory dir, ITime time)
        {
            _taskHelper = taskHelper;
            _time = time;
            _scheduler = new Scheduler(time, taskHelper);
            _phyBmData = new PhyBmData(taskHelper, downloader);
            _writer = new PhyBmData.FpnWriter(taskHelper, dir);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly ITime _time;
        private readonly Scheduler _scheduler;
        private readonly PhyBmData _phyBmData;
        private readonly PhyBmData.FpnWriter _writer;

        public async Task Start(bool startImmediately, CancellationToken ct = default(CancellationToken))
        {
            bool wait = !startImmediately;
            while (true)
            {
                if (wait)
                {
                    await _scheduler.ScheduleOne(Duration.FromMinutes(30), Duration.FromMinutes(20), ct);
                }
                wait = true;
                // Fetch for the next settlement period. This should be ready 10 minutes beforehand.
                Instant fetchTime = _time.GetCurrentInstant() + Duration.FromMinutes(30);
                var data = await _phyBmData.GetAsync(fetchTime.SettlementDate(), fetchTime.SettlementPeriod(), ct);
                await _writer.AppendAsync(data, ct);
            }
        }
    }
}

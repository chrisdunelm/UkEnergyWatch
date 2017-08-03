using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Applications
{
    public class FetchFreq
    {
        public FetchFreq(ITaskHelper taskHelper, IElexonDownloader downloader, IDirectory dir, ITime time)
        {
            _taskHelper = taskHelper;
            _time = time;
            _scheduler = new Scheduler(time, taskHelper);
            _freq = new Freq(taskHelper, downloader);
            _reader = new Freq.Reader(taskHelper, dir);
            _writer = new Freq.Writer(taskHelper, dir);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly ITime _time;
        private readonly Scheduler _scheduler;
        private readonly Freq _freq;
        private readonly Freq.Reader _reader;
        private readonly Freq.Writer _writer;

        public async Task Start(bool startImmediately, CancellationToken ct = default(CancellationToken))
        {
            bool wait = !startImmediately;
            while (true)
            {
                if (wait)
                {
                    await _scheduler.ScheduleOne(Duration.FromMinutes(2), Duration.FromSeconds(4), ct).ConfigureAwait(_taskHelper);
                }
                wait = true;
                var count = (int)await _reader.CountAsync(ct).ConfigureAwait(_taskHelper);
                Instant now = _time.GetCurrentInstant();
                Instant from;
                if (count > 0)
                {
                    var last = await (await _reader.ReadAsync(count - 1).ConfigureAwait(_taskHelper)).Last().ConfigureAwait(_taskHelper);
                    from = last.Update;
                }
                else
                {
                    from = now - Duration.FromHours(1);
                }
                Instant to = from + Duration.FromHours(1);
                if (to > now)
                {
                    to = now;
                }
                var data = await _freq.GetAsync(from + Duration.FromSeconds(1), to, ct).ConfigureAwait(_taskHelper);
                await _writer.AppendAsync(data, ct).ConfigureAwait(_taskHelper);
            }
        }
    }
}
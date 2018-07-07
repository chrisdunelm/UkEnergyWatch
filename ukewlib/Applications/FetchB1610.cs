using System;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Applications
{
    // B1610 – Actual Generation Output per Generation Unit
    public class FetchB1610
    {
        public FetchB1610(ITaskHelper taskHelper, IElexonDownloader downloader, IDirectory dir, ITime time,
            string errorLogFilename)
        {
            _taskHelper = taskHelper;
            _time = time;
            _scheduler = new Scheduler(time, taskHelper);
            _b1610 = new B1610(taskHelper, downloader);
            _reader = new B1610.Reader(taskHelper, dir);
            _writer = new B1610.Writer(taskHelper, dir);
            _errorLogFilename = errorLogFilename;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly ITime _time;
        private readonly Scheduler _scheduler;
        private readonly B1610 _b1610;
        private readonly B1610.Reader _reader;
        private readonly B1610.Writer _writer;
        private readonly string _errorLogFilename;

        public async Task Start(CancellationToken ct = default(CancellationToken))
        {
            while (true)
            {
                try
                {
                    await Start0(ct).ConfigureAwait(_taskHelper);
                }
                catch (Exception e) when (e.Is<IOException>() || e.Is<HttpRequestException>())
                {
                    if (_errorLogFilename != null)
                    {
                        File.WriteAllText(_errorLogFilename, e.ToString());
                    }
                    await _taskHelper.Delay(Duration.FromMinutes(45), ct).ConfigureAwait(_taskHelper);
                }
                catch (Exception e)
                {
                    if (_errorLogFilename != null)
                    {
                        File.WriteAllText(_errorLogFilename, e.ToString());
                    }
                    throw;
                }
            }
        }

        private async Task Start0(CancellationToken ct)
        {
            while (true)
            {
                int count = (int)await _reader.CountAsync().ConfigureAwait(_taskHelper);
                var items = await (await _reader.ReadAsync(count - 500, ct: ct).ConfigureAwait(_taskHelper)).ToList().ConfigureAwait(_taskHelper);
                var lastTime = items.Any() ? items.Max(x => x.SettlementPeriodStart) : _time.GetCurrentInstant() - Duration.FromDays(10);
                var fetchTime = lastTime + Duration.FromMinutes(30);
                var data = await _b1610.GetAsync(fetchTime.SettlementDate(), fetchTime.SettlementPeriod(), ct).ConfigureAwait(_taskHelper);
                if (data.Count == 0)
                {
                    await _scheduler.ScheduleOne(Duration.FromMinutes(30), Duration.FromMinutes(2.5), ct).ConfigureAwait(_taskHelper);
                }
                else
                {
                    await _writer.AppendAsync(data, ct).ConfigureAwait(_taskHelper);
                    await _scheduler.ScheduleOne(Duration.FromMinutes(5), Duration.FromMinutes(2.5), ct).ConfigureAwait(_taskHelper);
                }
            }
        }
    }
}

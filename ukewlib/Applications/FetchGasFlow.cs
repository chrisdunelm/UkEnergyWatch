using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.NationalGrid;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Applications
{
    public class FetchGasFlow
    {
        public FetchGasFlow(ITaskHelper taskHelper, ISoapDownloader downloader, IDirectory dir, ITime time)
        {
            _taskHelper = taskHelper;
            _time = time;
            _scheduler = new Scheduler(time, taskHelper);
            _flow = new InstantaneousFlow(taskHelper, downloader);
            _reader = new InstantaneousFlow.Reader(taskHelper, dir);
            _writer = new InstantaneousFlow.Writer(taskHelper, dir);
            _strings = new Strings(taskHelper, dir);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly ITime _time;
        private readonly Scheduler _scheduler;
        private readonly InstantaneousFlow _flow;
        private readonly InstantaneousFlow.Reader _reader;
        private readonly InstantaneousFlow.Writer _writer;
        private readonly Strings _strings;

        public async Task Start(bool startImmediately, CancellationToken ct = default(CancellationToken))
        {
            bool wait = !startImmediately;
            while (true)
            {
                if (wait)
                {
                    await _scheduler.ScheduleOne(Duration.FromMinutes(12), Duration.FromMinutes(2.2), ct).ConfigureAwait(_taskHelper);
                }
                wait = true;
                var lastData = await (await _reader.ReadAsync((int)await _reader.CountAsync(ct).ConfigureAwait(_taskHelper) - 1, ct: ct)
                    .ConfigureAwait(_taskHelper)).FirstOrDefault(ct).ConfigureAwait(_taskHelper);
                var tries = 50; // 10 minutes
                while (--tries >= 0 && await _flow.GetLatestPublicationTimeAsync(ct).ConfigureAwait(_taskHelper) < lastData.Update)
                {
                    await _taskHelper.Delay(Duration.FromMinutes(0.2)).ConfigureAwait(_taskHelper);
                }
                if (tries >= 0)
                {
                    var datas0 = await _flow.GetInstantaneousFlowDataAsync(ct).ConfigureAwait(_taskHelper);
                    var datas1 = await datas0.WriteStringsAsync(_taskHelper, _strings).ConfigureAwait(_taskHelper);
                    await _writer.AppendAsync(datas1, ct).ConfigureAwait(_taskHelper);
                }
            }
        }
    }
}

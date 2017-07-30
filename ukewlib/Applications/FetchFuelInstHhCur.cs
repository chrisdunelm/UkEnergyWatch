using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Applications
{
    public class FetchFuelInstHhCur
    {
        private static Duration s_downloadInterval = Duration.FromMinutes(5);
        private static Duration s_downloadOffset = Duration.FromMinutes(0.9);

        public FetchFuelInstHhCur(ITaskHelper taskHelper, IElexonDownloader downloader, IDirectory dir, ITime time)
        {
            _taskHelper = taskHelper;
            _scheduler = new Scheduler(time, taskHelper);
            _fuelInstHhCur = new FuelInstHhCur(taskHelper, downloader);
            _datastoreWriter = new DataStoreWriter<FuelInstHhCur.Data, FuelInstHhCur.Data>(taskHelper, dir);
        }

        private ITaskHelper _taskHelper;
        private Scheduler _scheduler;
        private FuelInstHhCur _fuelInstHhCur;
        private DataStoreWriter<FuelInstHhCur.Data, FuelInstHhCur.Data> _datastoreWriter;

        public async Task Start(CancellationToken ct)
        {
            while (true)
            {
                await _scheduler.ScheduleOne(s_downloadInterval, s_downloadOffset, ct).ConfigureAwait(_taskHelper);
                var data = await _fuelInstHhCur.GetAsync(ct).ConfigureAwait(_taskHelper);
                await _datastoreWriter.AppendAsync(data, ct).ConfigureAwait(_taskHelper);
            }
        }
    }
}

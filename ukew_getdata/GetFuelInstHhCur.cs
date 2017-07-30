using System;
using System.Threading.Tasks;
using CommandLine;
using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew
{
    public class GetFuelInstHhCur
    {
        [Verb("GetFuelInstHhCur", HelpText = "Fuel-instance Half-hour current. Updates every 5 minutes")]
        public class Options
        {
            [Option(Required = true, HelpText = "Elexon API key")]
            public string ElexonApiKey { get; set; }

            [Option(Required = true, HelpText = "Absolute or relative directory path for fuel-instance half-hour storage")]
            public string DataDirectory { get; set; }
        }

        public GetFuelInstHhCur(Options opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            _scheduler = new Scheduler(SystemTime.Instance, taskHelper);
            var elexonDownloader = new ElexonDownloader(taskHelper, opts.ElexonApiKey);
            _fuelInstHhCur = new FuelInstHhCur(taskHelper, elexonDownloader);
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            _datastoreWriter = new DataStoreWriter<FuelInstHhCur.Data, FuelInstHhCur.Data>(taskHelper, dir);
        }

        private readonly Scheduler _scheduler;
        private readonly FuelInstHhCur _fuelInstHhCur;
        private readonly DataStoreWriter<FuelInstHhCur.Data, FuelInstHhCur.Data> _datastoreWriter;

        public async Task<int> Run()
        {
            // TODO: Logging
            Console.WriteLine("Starting GetFuelInstHhCur");
            while (true)
            {
                await _scheduler.ScheduleOne(Duration.FromMinutes(5), Duration.FromMinutes(1.8));
                Console.WriteLine("About to get data");
                try
                {
                    var data = await _fuelInstHhCur.GetAsync();
                    await _datastoreWriter.AppendAsync(data);
                }
                catch (Exception e)
                {
                    // TODO: Logging
                    Console.WriteLine("Error getting or storing data:");
                    Console.WriteLine(e);
                    Console.WriteLine();
                }
            }
        }
    }
}

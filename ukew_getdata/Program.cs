using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using CommandLine;
using NodaTime;
using Ukew.Applications;
using Ukew.Elexon;
using Ukew.NationalGrid;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew
{
    public static class Program
    {
        public static int Main(string[] args)
        {
            return Parser.Default.ParseArguments(args,
                typeof(GetFuelInstHhCur.Options), typeof(ShowFuelInstHhCur.Options),
                typeof(GetFreqOptions), typeof(ShowFreqOptions),
                typeof(GetPhyBmDataOptions), typeof(ShowPhyBmDataOptions),
                typeof(GetB1610Options), typeof(ShowB1610Options),
                typeof(GetGasFlowOptions), typeof(ShowGasFlowOptions))
                .MapResult(
                    (GetFuelInstHhCur.Options opts) => Task.Run(() => new GetFuelInstHhCur(opts).Run()).Result,
                    (ShowFuelInstHhCur.Options opts) => Task.Run(() => new ShowFuelInstHhCur(opts).Run()).Result,
                    (GetFreqOptions opts) => Task.Run(() => GetFreq(opts)).Result,
                    (ShowFreqOptions opts) => Task.Run(() => ShowFreq(opts)).Result,
                    (GetPhyBmDataOptions opts) => Task.Run(() => GetPhyBmData(opts)).Result,
                    (ShowPhyBmDataOptions opts) => Task.Run(() => ShowPhyBmData(opts)).Result,
                    (GetB1610Options opts) => Task.Run(() => GetB1610(opts)).Result,
                    (ShowB1610Options opts) => Task.Run(() => ShowB1610(opts)).Result,
                    (GetGasFlowOptions opts) => Task.Run(() => GetGasFlow(opts)).Result,
                    (ShowGasFlowOptions opts) => Task.Run(() => ShowGasFlow(opts)).Result,
                    errs => 1
                );
        }

        [Verb("GetFreq", HelpText = "Fetch grid frequency. Updates every two minutes.")]
        class GetFreqOptions
        {
            [Option(Required = true, HelpText = "Elexon API key")]
            public string ElexonApiKey { get; set; }

            [Option(Required = true, HelpText = "Absolute or relative directory path for frequency storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Perform an initial Get immediately, don't schedule a wait")]
            public bool GetImmediately { get; set; } = false;
        }

        static async Task<int> GetFreq(GetFreqOptions opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var downloader = new ElexonDownloader(taskHelper, opts.ElexonApiKey);
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var fetch = new FetchFreq(taskHelper, downloader, dir, SystemTime.Instance);
            await fetch.Start(opts.GetImmediately);
            return 0;
        }

        [Verb("ShowFreq", HelpText="Show existing grid frequency data.")]
        class ShowFreqOptions
        {
            [Option(Required = true, HelpText = "Absolute or relative directory path for grid frequency storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Count of tail data items to show")]
            public int Count { get; set; } = 10;
        }

        static async Task<int> ShowFreq(ShowFreqOptions opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var reader = new Freq.Reader(taskHelper, dir);
            var count = opts.Count;
            var totalCount = (int)await reader.CountAsync();
            Console.WriteLine($"Grid Frequency count: {totalCount}");
            Console.WriteLine($"Latest {count} data readings:");
            var data = await reader.ReadAsync(totalCount - count, totalCount);
            data.ForEach(r => Console.WriteLine(r));
            return 0;
        }

        [Verb("GetPhyBmData", HelpText = "Fetch Physical BM Data. Updates every 30 minutes.")]
        class GetPhyBmDataOptions
        {
            [Option(Required = true, HelpText = "Elexon API key")]
            public string ElexonApiKey { get; set; }

            [Option(Required = true, HelpText = "Absolute or relative directory path for physical BM data storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Perform an initial Get immediately, don't schedule a wait")]
            public bool GetImmediately { get; set; } = false;
        }

        static async Task<int> GetPhyBmData(GetPhyBmDataOptions opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var downloader = new ElexonDownloader(taskHelper, opts.ElexonApiKey);
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var fetch = new FetchPhyBmData(taskHelper, downloader, dir, SystemTime.Instance);
            await fetch.Start(opts.GetImmediately);
            return 0;
        }

        [Verb("ShowPhyBmData", HelpText="Show existing Physical BM data data.")]
        class ShowPhyBmDataOptions
        {
            [Option(Required = true, HelpText = "Absolute or relative directory path for physical BM data storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Count of tail data items to show")]
            public int Count { get; set; } = 10;
        }

        static async Task<int> ShowPhyBmData(ShowPhyBmDataOptions opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var reader = new PhyBmData.FpnReader(taskHelper, dir);
            var count = opts.Count;
            var totalCount = (int)await reader.CountAsync();
            Console.WriteLine($"Physical BM data count: {totalCount}");
            Console.WriteLine($"Latest {count} data readings:");
            var data = await reader.ReadAsync(totalCount - count, totalCount);
            data.ForEach(r => Console.WriteLine(r));
            return 0;
        }

        [Verb("GetB1610", HelpText = "Fetch B1610 data (actual per-unit generation). Updates every 30 minutes.")]
        class GetB1610Options
        {
            [Option(Required = true, HelpText = "Elexon API key")]
            public string ElexonApiKey { get; set; }

            [Option(Required = true, HelpText = "Absolute or relative directory path for B1610 data storage")]
            public string DataDirectory { get; set; }
        }

        static async Task<int> GetB1610(GetB1610Options opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var downloader = new ElexonDownloader(taskHelper, opts.ElexonApiKey);
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var fetch = new FetchB1610(taskHelper, downloader, dir, SystemTime.Instance);
            await fetch.Start();
            return 0;
        }

        [Verb("ShowB1610", HelpText="Show existing B1610 (actual per-unit generation) data.")]
        class ShowB1610Options
        {
            [Option(Required = true, HelpText = "Absolute or relative directory path for B1610 data storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Count of tail data items to show")]
            public int Count { get; set; } = 10;
        }

        static async Task<int> ShowB1610(ShowB1610Options opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var reader = new B1610.Reader(taskHelper, dir);
            var count = opts.Count;
            var totalCount = (int)await reader.CountAsync();
            Console.WriteLine($"B1610 data count: {totalCount}");
            Console.WriteLine($"Latest {count} data readings:");
            var data = await reader.ReadAsync(totalCount - count, totalCount);
            data.ForEach(r => Console.WriteLine(r));
            return 0;
        }

        [Verb("GetGasFlow", HelpText = "Fetch gas flow data. Updates every 12 minutes.")]
        class GetGasFlowOptions
        {
            [Option(Required = true, HelpText = "Absolute or relative directory path for gas flow data storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Perform an initial Get immediately, don't schedule a wait")]
            public bool GetImmediately { get; set; } = false;
        }

        static async Task<int> GetGasFlow(GetGasFlowOptions opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var downloader = new SoapDownloader(taskHelper);
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var fetch = new FetchGasFlow(taskHelper, downloader, dir, SystemTime.Instance);
            await fetch.Start(opts.GetImmediately);
            return 0;
        }

        [Verb("ShowGasFlow", HelpText = "Show existing gas flow data.")]
        class ShowGasFlowOptions
        {
            [Option(Required = true, HelpText = "Absolute or relative directory path for gas flow data storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Count of tail data items to show")]
            public int Count { get; set; } = 10;
        }

        static async Task<int> ShowGasFlow(ShowGasFlowOptions opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            var reader = new InstantaneousFlow.Reader(taskHelper, dir);
            var strings = new Strings(taskHelper, dir);
            var count = opts.Count;
            var totalCount = (int)await reader.CountAsync();
            Console.WriteLine($"Gas-flow data count: {totalCount}");
            Console.WriteLine($"Latest {count} data readings:");
            var data = await reader.ReadAsync(totalCount - count, totalCount);
            data.ForEach(r => Console.WriteLine(r.ToString(strings)));
            return 0;
        }

    }
}

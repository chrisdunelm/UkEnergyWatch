using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using CommandLine;
using NodaTime;
using Ukew.Applications;
using Ukew.Elexon;
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
                typeof(GetPhyBmDataOptions), typeof(ShowPhyBmDataOptions))
                .MapResult(
                    (GetFuelInstHhCur.Options opts) => Task.Run(() => new GetFuelInstHhCur(opts).Run()).Result,
                    (ShowFuelInstHhCur.Options opts) => Task.Run(() => new ShowFuelInstHhCur(opts).Run()).Result,
                    (GetFreqOptions opts) => Task.Run(() => GetFreq(opts)).Result,
                    (ShowFreqOptions opts) => Task.Run(() => ShowFreq(opts)).Result,
                    (GetPhyBmDataOptions opts) => Task.Run(() => GetPhyBmData(opts)).Result,
                    (ShowPhyBmDataOptions opts) => Task.Run(() => ShowPhyBmData(opts)).Result,
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
    }
}

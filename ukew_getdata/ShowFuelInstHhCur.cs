using System;
using System.Linq;
using System.Threading.Tasks;
using CommandLine;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils.Tasks;
using UnitsNet;
using UnitsNet.Units;

namespace Ukew
{
    public class ShowFuelInstHhCur
    {
        [Verb("ShowFuelInstHhCur", HelpText = "Show existing Fuel-instance Half-hour current data.")]
        public class Options
        {
            [Option(Required = true, HelpText = "Absolute or relative directory path for fuel-instance half-hour storage")]
            public string DataDirectory { get; set; }

            [Option(Required = false, HelpText = "Count of tail data items to show")]
            public int Count { get; set; } = 10;
        }

        public ShowFuelInstHhCur(Options opts)
        {
            var taskHelper = SystemTaskHelper.Instance;
            var dir = new SystemDirectory(taskHelper, opts.DataDirectory);
            _datastoreReader = new FuelInstHhCur.Reader(taskHelper, dir);
            _count = opts.Count;
        }

        private readonly DataStoreReader<FuelInstHhCur.Data, FuelInstHhCur.Data> _datastoreReader;
        private readonly int _count;

        public async Task<int> Run()
        {
            var totalCount = (int)await _datastoreReader.CountAsync();
            Console.WriteLine($"Fuelinst half-hour count: {totalCount}");
            Console.WriteLine($"Latest {_count} data readings:");
            var data = await _datastoreReader.ReadAsync(totalCount - _count, totalCount);
            string MW(Power p) => p.ToString(PowerUnit.Megawatt);
            data.ForEach(r =>
            {
                Console.WriteLine($"[{r.Update}] Ccgt:{MW(r.Ccgt)} Ocgt:{MW(r.Ocgt)} Oil:{MW(r.Oil)} Coal:{MW(r.Coal)} " +
                    $"Nuclear:{MW(r.Nuclear)} Wind:{MW(r.Wind)} Ps:{MW(r.Ps)} NPsHyd:{MW(r.Npshyd)} Other:{MW(r.Other)} " +
                    $"IntFr:{MW(r.IntFr)} IntIrl:{MW(r.IntIrl)} IntNed:{MW(r.IntNed)} IntEw:{MW(r.IntEw)} = {MW(r.Total)}");
            });
            return 0;
        }
    }
}

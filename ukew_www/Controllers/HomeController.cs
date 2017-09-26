using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using NodaTime;
using Ukew.Elexon;
using Ukew.NationalGrid;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using UnitsNet;

namespace ukew_www.Controllers
{
    public class HomeController : Controller
    {
        public class IndexModel
        {
            public class FuelInstHhCurModel
            {
                public class Row
                {
                    public Row(string name, Power power, MassFlow? co2 = null)
                    {
                        Name = name;
                        Power = power;
                        Co2 = co2;
                    }
                    public string Name { get; }
                    public Power Power { get; }
                    public MassFlow? Co2 { get; }
                }

                public FuelInstHhCurModel(IEnumerable<Row> rows, Row total, double co2KgPerKwh, ZonedDateTime updateTime)
                {
                    Rows = rows;
                    Total = total;
                    Co2KgPerKwh = co2KgPerKwh;
                    UpdateTime = updateTime;
                }
                public IEnumerable<Row> Rows { get; }
                public Row Total { get; }
                public double Co2KgPerKwh { get; }
                public ZonedDateTime UpdateTime { get; }
            }

            public class FrequencyModel
            {
                public FrequencyModel(Frequency frequency, ZonedDateTime updateTime)
                {
                    Frequency = frequency;
                    UpdateTime = updateTime;
                }
                public Frequency Frequency { get; }
                public ZonedDateTime UpdateTime { get; }
            }

            public class GasFlowModel
            {
                public class Row
                {
                    public Row(string name, Flow gasFlow, MassFlow co2, InstantaneousFlow.SupplyType supplyType)
                    {
                        Name = name;
                        GasFlow = gasFlow;
                        Co2 = co2;
                        SupplyType = supplyType;
                    }
                    public string Name { get; }
                    public Flow GasFlow { get; }
                    public MassFlow Co2 { get; }
                    public InstantaneousFlow.SupplyType SupplyType { get; }
                }

                public GasFlowModel(IEnumerable<Row> rows, Row total, ZonedDateTime updateTime)
                {
                    Rows = rows;
                    Total = total;
                    UpdateTime = updateTime;
                }

                public IEnumerable<Row> Rows { get; }
                public Row Total { get; }
                public ZonedDateTime UpdateTime { get; }
            }

            public IndexModel(FuelInstHhCurModel fuelInstHhCurData, FrequencyModel frequencyData, GasFlowModel gasFlowData)
            {
                FuelInstHhCurData = fuelInstHhCurData;
                FrequencyData = frequencyData;
                GasFlowData = gasFlowData;
            }

            public FuelInstHhCurModel FuelInstHhCurData { get; }
            public FrequencyModel FrequencyData { get; }
            public GasFlowModel GasFlowData { get; }
        }

        public class PowerStationsModel
        {
            public class GenModel
            {
                public class Generation
                {
                    public Power? Power { get; set; }
                    public ZonedDateTime? UpdateTime { get; set; }
                    public MassFlow? Co2 { get; set; }
                }

                public class GeneratingUnit
                {
                    public EicIds.GenerationUnit UnderlyingGenerationUnit { get; set; }
                    public Generation FpnGeneration { get; set; }
                    public Generation B1610Generation { get; set; }
                }

                public class PowerStation
                {
                    public EicIds.PowerStation UnderlyingPowerStation { get; set; }
                    public Generation FpnGeneration { get; set; }
                    public Generation B1610Generation { get; set; }
                    public IEnumerable<GeneratingUnit> GeneratingUnits { get; set; }
                    public string CssId { get; set; }
                }

                public GenModel(IEnumerable<IGrouping<EicIds.FuelType, PowerStation>> powerStationsByFuel)
                {
                    PowerStationsByFuel = powerStationsByFuel;
                }

                public IEnumerable<IGrouping<EicIds.FuelType, PowerStation>> PowerStationsByFuel { get; }
            }

            public PowerStationsModel(GenModel genData)
            {
                GenData = genData;
            }

            public GenModel GenData { get; }
        }

        public HomeController(ITime time, FuelInstHhCur.Reader fuelInstHhCurReader, Freq.Reader freqReader,
            PhyBmData.FpnReader fpnReader, B1610.Reader b1610Reader, B1610Seen b1610Seen, InstantaneousFlow.Reader gasFlowReader)
        {
            _time = time;
            _fuelInstHhCurReader = fuelInstHhCurReader;
            _freqReader = freqReader;
            _fpnReader = fpnReader;
            _b1610Reader = b1610Reader;
            _b1610Seen = b1610Seen;
            _gasFlowReader = gasFlowReader;
        }

        private readonly ITime _time;
        private readonly FuelInstHhCur.Reader _fuelInstHhCurReader;
        private readonly Freq.Reader _freqReader;
        private readonly PhyBmData.FpnReader _fpnReader;
        private readonly B1610.Reader _b1610Reader;
        private readonly B1610Seen _b1610Seen;
        private readonly InstantaneousFlow.Reader _gasFlowReader;

        private static readonly DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

        [HttpGet("/")]
        public async Task<IActionResult> Index()
        {
            var fuelInstHhCurData = await GetIndexFuelInstHhCurDataAsync();
            var frequencyData = await GetIndexFrequencyDataAsync();
            var gasFlowData = await GetIndexGasFlowDataAsync();
            var model = new IndexModel(fuelInstHhCurData, frequencyData, gasFlowData);
            return View(model);
        }

        private async Task<IndexModel.FuelInstHhCurModel> GetIndexFuelInstHhCurDataAsync()
        {
            var count = (int)await _fuelInstHhCurReader.CountAsync();
            var data = await _fuelInstHhCurReader.ReadAsync(count - 1, count);
            var latestData = await data.FirstOrDefault();
            var rows = new []
            {
                new IndexModel.FuelInstHhCurModel.Row("Combined Cycle Gas Turbine", latestData.Ccgt, latestData.Ccgt.CalculateCo2(1.02006335797254e-7)),
                new IndexModel.FuelInstHhCurModel.Row("Open Cycle Gas Turbine", latestData.Ocgt, latestData.Ocgt.CalculateCo2(0)), // TODO: CO2
                new IndexModel.FuelInstHhCurModel.Row("Oil", latestData.Oil, latestData.Oil.CalculateCo2(0)), // TODO: CO2
                new IndexModel.FuelInstHhCurModel.Row("Coal", latestData.Coal, latestData.Coal.CalculateCo2(2.71604938271605e-7)),
                new IndexModel.FuelInstHhCurModel.Row("Nuclear", latestData.Nuclear, MassFlow.Zero),
                new IndexModel.FuelInstHhCurModel.Row("Wind", latestData.Wind, MassFlow.Zero),
                new IndexModel.FuelInstHhCurModel.Row("Pumped Storage Hydro", latestData.Ps, MassFlow.Zero),
                new IndexModel.FuelInstHhCurModel.Row("Non Pumped Storage Hydro", latestData.Npshyd, MassFlow.Zero),
                new IndexModel.FuelInstHhCurModel.Row("Other", latestData.Other),
                new IndexModel.FuelInstHhCurModel.Row("Interconnect - France", latestData.IntFr),
                new IndexModel.FuelInstHhCurModel.Row("Interconnect - Ireland (Moyle)", latestData.IntIrl),
                new IndexModel.FuelInstHhCurModel.Row("Interconnect - Netherlands", latestData.IntNed),
                new IndexModel.FuelInstHhCurModel.Row("Interconnect - Ireland (East-West)", latestData.IntEw),
            };
            var totalCo2 = MassFlow.FromKilogramsPerSecond(rows.Sum(x => x.Co2?.KilogramsPerSecond)).Value;
            var total = new IndexModel.FuelInstHhCurModel.Row("Total", latestData.Total, totalCo2);
            var co2KgPerKwh = totalCo2.KilogramsPerHour / latestData.Total.Kilowatts;
            var updateTime = latestData.Update.InZone(s_tzLondon);
            return new IndexModel.FuelInstHhCurModel(rows, total, co2KgPerKwh, updateTime);
        }

        private async Task<IndexModel.FrequencyModel> GetIndexFrequencyDataAsync()
        {
            var freqCount = (int)await _freqReader.CountAsync();
            var freqData = await _freqReader.ReadAsync(freqCount - 1, freqCount);
            var freqLatest = await freqData.FirstOrDefault();
            return new IndexModel.FrequencyModel(freqLatest.Frequency, freqLatest.Update.InZone(s_tzLondon));
        }

        private async Task<IndexModel.GasFlowModel> GetIndexGasFlowDataAsync(bool includeZones = false)
        {
            var terminalSupply = InstantaneousFlow.SupplyType.TerminalSupply;
            var count = (int)await _gasFlowReader.CountAsync();
            var datasAsync = await _gasFlowReader.ReadAsync(count - 500, count);
            var datas = await datasAsync.ToList();
            var lastTotal = datas
                .Where(x => x.Type == InstantaneousFlow.SupplyType.TotalSupply)
                .OrderByDescending(x => x.Update)
                .FirstOrDefault();
            var terminals = datas
                .Where(x => x.Type == terminalSupply && x.Update == lastTotal.Update);
            var strings = _gasFlowReader.Strings;
            var rows = await terminals
                .SelectAsync(SystemTaskHelper.Instance, async x =>
                    new IndexModel.GasFlowModel.Row((await x.NameAsync(strings)).ToUpperInvariant(), x.FlowRate, MassFlow.Zero, terminalSupply));
            if (includeZones)
            {
                var zoneSupply = InstantaneousFlow.SupplyType.ZoneSupply;
                var zoneRows = await datas
                    .Where(x => x.Type == zoneSupply && x.Update == lastTotal.Update)
                    .SelectAsync(SystemTaskHelper.Instance, async x =>
                        new IndexModel.GasFlowModel.Row((await x.NameAsync(strings)).ToUpperInvariant(), x.FlowRate, MassFlow.Zero, zoneSupply));
                rows = rows.Concat(zoneRows);
            }
            var totalRow = new IndexModel.GasFlowModel.Row("Total", lastTotal.FlowRate, MassFlow.Zero, InstantaneousFlow.SupplyType.TotalSupply);
            return new IndexModel.GasFlowModel(rows, totalRow, lastTotal.Update.InZone(s_tzLondon));
        }

        [HttpGet("/powerstations")]
        public async Task<IActionResult> PowerStations()
        {
            var model = new PowerStationsModel(await MakeGenModel());
            return View(model);
        }

        private async Task<PowerStationsModel.GenModel> MakeGenModel()
        {
            var fpnCount = (int)await _fpnReader.CountAsync();
            var fpnData = await (await _fpnReader.ReadAsync(fpnCount - 2000, fpnCount)).ToList();
            var b1610Count = (int)await _b1610Reader.CountAsync();
            var b1610Data = await (await _b1610Reader.ReadAsync(b1610Count - 1000, b1610Count)).ToList();
            var allResourceNames = new HashSet<string>(EicIds.GenerationUnits.Select(x => x.RegisteredResourceName));
            var genUnitFpn = new Dictionary<string, (Power power, Instant update)>();
            var genUnitB1610 = new Dictionary<string, (Power power, Instant update)>();
            Instant now = _time.GetCurrentInstant();
            foreach (var fpn in fpnData.Where(x => now >= x.TimeFrom))
            {
                Power? level = fpn.LevelAt(now);
                genUnitFpn[fpn.ResourceName] = (level ?? fpn.LevelTo, level != null ? now : fpn.TimeTo);
            }
            foreach (var b1610 in b1610Data)
            {
                genUnitB1610[b1610.ResourceName] = (b1610.Power, b1610.SettlementPeriodStart + NodaTime.Duration.FromMinutes(30));
            }
            var lastInstant = b1610Data.Select(x => x.SettlementPeriodStart).Max();
            foreach (var b1610 in _b1610Seen.AllResourceNames)
            {
                if (!genUnitB1610.ContainsKey(b1610))
                {
                    genUnitB1610.Add(b1610, (Power.Zero, lastInstant));
                }
            }

            // TODO: Put this somewhere better
            var co2KgPerJoules = new Dictionary<EicIds.FuelType, double>
            {
                { EicIds.FuelType.Coal, 2.71604938271605e-7 },
                { EicIds.FuelType.Ccgt, 1.02006335797254e-7 },
            };

            var fuelTypeOrder = new Dictionary<EicIds.FuelType, int>
            {
                { EicIds.FuelType.Ccgt, 1 },
                { EicIds.FuelType.Ocgt, 2 },
                { EicIds.FuelType.Oil, 3 },
                { EicIds.FuelType.Coal, 4 },
                { EicIds.FuelType.Nuclear, 5 },
                { EicIds.FuelType.WindOnshore, 6 },
                { EicIds.FuelType.WindOffshore, 7 },
                { EicIds.FuelType.Wind, 8 },
                { EicIds.FuelType.Solar, 9 },
                { EicIds.FuelType.PumpedStorage, 10 },
                { EicIds.FuelType.Hydro, 11 },
                { EicIds.FuelType.Other, 12 },
                { EicIds.FuelType.Unknown, 13 },
            };

            var ret = EicIds.PowerStations
                .Where(x => x.GenerationUnitRegisteredResourceNames.Any())
                .Where(x => !EicIds.InactiveResourceNames.Contains(x.RegisteredResourceName))
                .Select((powerStation, psIndex) =>
                {
                    var co2KgPerJoule = co2KgPerJoules.TryGetValue(powerStation.FuelType, out var v) ? v : 0.0;
                    var psGenUnits = powerStation.GenerationUnitRegisteredResourceNames
                        .Select(resourceName =>
                        {
                            PowerStationsModel.GenModel.Generation MakeGen(Dictionary<string, (Power power, Instant update)> data)
                            {
                                var haveData = data.TryGetValue(resourceName, out var data1);
                                return new PowerStationsModel.GenModel.Generation
                                {
                                    Power = haveData ? data1.power : (Power?)null,
                                    UpdateTime = haveData ? data1.update.InZone(s_tzLondon) : (ZonedDateTime?)null,
                                    Co2 = haveData ? data1.power.CalculateCo2(co2KgPerJoule) : (MassFlow?)null,
                                };
                            }
                            return new PowerStationsModel.GenModel.GeneratingUnit
                            {
                                UnderlyingGenerationUnit = EicIds.GenerationUnitsByResourceName[resourceName],
                                FpnGeneration = MakeGen(genUnitFpn),
                                B1610Generation = MakeGen(genUnitB1610),
                            };
                        })
                        .ToList();
                    PowerStationsModel.GenModel.Generation MakePsGen(Func<PowerStationsModel.GenModel.GeneratingUnit, PowerStationsModel.GenModel.Generation> fn, bool needsAll)
                    {
                        var genUnitData = psGenUnits.Select(fn).ToList();
                        var allGenUnitsHaveData = needsAll ? genUnitData.All(x => x.Power != null) : genUnitData.Any(x => x.Power != null);
                        return new PowerStationsModel.GenModel.Generation
                        {
                            Power = allGenUnitsHaveData ? genUnitData.Aggregate(Power.Zero, (total, x) => total + (x.Power ?? Power.Zero)) : (Power?)null,
                            UpdateTime = allGenUnitsHaveData ? genUnitData.FirstOrDefault(x => x.UpdateTime.HasValue)?.UpdateTime : (ZonedDateTime?)null,
                            Co2 = allGenUnitsHaveData ? genUnitData.Aggregate(MassFlow.Zero, (total, x) => total + (x.Co2 ?? MassFlow.Zero)) : (MassFlow?)null,
                        };
                    }
                    return new PowerStationsModel.GenModel.PowerStation
                    {
                        UnderlyingPowerStation = powerStation,
                        FpnGeneration = MakePsGen(x => x.FpnGeneration, true),
                        B1610Generation = MakePsGen(x => x.B1610Generation, false),
                        GeneratingUnits = psGenUnits,
                        CssId = $"__ps_{psIndex}",
                    };
                })
                .OrderBy(x => x.UnderlyingPowerStation.RegisteredResourceName)
                .GroupBy(x => x.UnderlyingPowerStation.FuelType)
                .OrderBy(x => fuelTypeOrder[x.Key])
                .ToList();
            return new PowerStationsModel.GenModel(ret);
        }

        [HttpGet("/gasinflow")]
        public async Task<IActionResult> GasInFlow()
        {
            var model = await GetIndexGasFlowDataAsync(includeZones: true);
            return View(model);
        }

        [HttpGet("/contact")]
        public IActionResult Contact()
        {
            return View();
        }

        public IActionResult Error()
        {
            return View();
        }
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using NodaTime;
using Ukew.Elexon;
using Ukew.Utils;
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

            public IndexModel(FuelInstHhCurModel fuelInstHhCurData, FrequencyModel frequencyData)
            {
                FuelInstHhCurData = fuelInstHhCurData;
                FrequencyData = frequencyData;
            }

            public FuelInstHhCurModel FuelInstHhCurData { get; }
            public FrequencyModel FrequencyData { get; }
        }

        public class PowerStationsModel
        {
            public class FpnModel
            {
                public class GeneratingUnit
                {
                    public EicIds.GenerationUnit UnderlyingGenerationUnit { get; set; }
                    public Power? CurrentGeneration { get; set; }
                    public ZonedDateTime? UpdateTime { get; set; }
                    public MassFlow? Co2 { get; set; }
                }

                public class PowerStation
                {
                    public EicIds.PowerStation UnderlyingPowerStation { get; set; }
                    public Power? CurrentGeneration { get; set; }
                    public ZonedDateTime? UpdateTime { get; set; }
                    public IEnumerable<GeneratingUnit> GeneratingUnits { get; set; }
                    public MassFlow? Co2 { get; set; }
                    public string CssId { get; set; }
                }

                public FpnModel(IEnumerable<IGrouping<EicIds.FuelType, PowerStation>> powerStationsByFuel)
                {
                    PowerStationsByFuel = powerStationsByFuel;
                }

                public IEnumerable<IGrouping<EicIds.FuelType, PowerStation>> PowerStationsByFuel { get; }
            }

            public PowerStationsModel(FpnModel fpnData)
            {
                FpnData = fpnData;
            }

            public FpnModel FpnData { get; }
        }

        public HomeController(ITime time, FuelInstHhCur.Reader fuelInstHhCurReader, Freq.Reader freqReader, PhyBmData.FpnReader fpnReader)
        {
            _time = time;
            _fuelInstHhCurReader = fuelInstHhCurReader;
            _freqReader = freqReader;
            _fpnReader = fpnReader;
        }

        private readonly ITime _time;
        private readonly FuelInstHhCur.Reader _fuelInstHhCurReader;
        private readonly Freq.Reader _freqReader;
        private readonly PhyBmData.FpnReader _fpnReader;

        private static readonly DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];


        [HttpGet("/")]
        public async Task<IActionResult> Index()
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
            var fuelInstHhCurData = new IndexModel.FuelInstHhCurModel(rows, total, co2KgPerKwh, updateTime);
            var freqCount = (int)await _freqReader.CountAsync();
            var freqData = await _freqReader.ReadAsync(freqCount - 1, freqCount);
            var freqLatest = await freqData.FirstOrDefault();
            var frequencyData = new IndexModel.FrequencyModel(freqLatest.Frequency, freqLatest.Update.InZone(s_tzLondon));
            var model = new IndexModel(fuelInstHhCurData, frequencyData);
            return View(model);
        }

        [HttpGet("/powerstations")]
        public async Task<IActionResult> PowerStations()
        {
            var model = new PowerStationsModel(await MakeFpnModel());
            return View(model);
        }

        private async Task<PowerStationsModel.FpnModel> MakeFpnModel()
        {
            var count = (int)await _fpnReader.CountAsync();
            var fpnData = await (await _fpnReader.ReadAsync(count - 2000, count)).ToList();
            fpnData.Reverse();
            var resourceNamesToFind = new HashSet<string>(EicIds.GenerationUnits.Select(x => x.RegisteredResourceName));
            var genUnitPower = new Dictionary<string, (Power power, Instant update)>();
            Instant now = _time.GetCurrentInstant();
            foreach (var fpn in fpnData)
            {
                if (resourceNamesToFind.Contains(fpn.ResourceName))
                {
                    if (now >= fpn.TimeFrom)
                    {
                        Power? level = fpn.LevelAt(now);
                        genUnitPower.Add(fpn.ResourceName, (level ?? fpn.LevelTo, level != null ? now : fpn.TimeTo));
                        resourceNamesToFind.Remove(fpn.ResourceName);
                        if (resourceNamesToFind.Count == 0)
                        {
                            break;
                        }
                    }
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
                            var genUnit = EicIds.GenerationUnitsByResourceName[resourceName];
                            var haveData = genUnitPower.ContainsKey(resourceName);
                            var curGen = haveData ? genUnitPower[resourceName].power : (Power?)null;
                            return new PowerStationsModel.FpnModel.GeneratingUnit
                            {
                                UnderlyingGenerationUnit = genUnit,
                                CurrentGeneration = curGen,
                                UpdateTime = haveData ? genUnitPower[resourceName].update.InZone(s_tzLondon) : (ZonedDateTime?)null,
                                Co2 = haveData ? curGen.Value.CalculateCo2(co2KgPerJoule) : (MassFlow?)null,
                            };
                        })
                        .ToList();
                    var haveAllData = psGenUnits.All(x => x.CurrentGeneration.HasValue);
                    return new PowerStationsModel.FpnModel.PowerStation
                    {
                        UnderlyingPowerStation = powerStation,
                        CurrentGeneration = haveAllData ? psGenUnits.Aggregate(Power.Zero, (total, x) => total + x.CurrentGeneration.Value) : (Power?)null,
                        UpdateTime = haveAllData ? psGenUnits.OrderBy(x => x.UpdateTime.Value.ToInstant()).First().UpdateTime : (ZonedDateTime?)null,
                        GeneratingUnits = psGenUnits,
                        Co2 = haveAllData ? psGenUnits.Aggregate(MassFlow.Zero, (total, x) => total + x.Co2.Value) : (MassFlow?)null,
                        CssId = $"__ps_{psIndex}",
                    };
                })
                .OrderBy(x => x.UnderlyingPowerStation.RegisteredResourceName)
                .GroupBy(x => x.UnderlyingPowerStation.FuelType)
                .OrderBy(x => fuelTypeOrder[x.Key])
                .ToList();
            return new PowerStationsModel.FpnModel(ret);
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

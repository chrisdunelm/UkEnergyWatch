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

        public HomeController(FuelInstHhCur.Reader fuelInstHhCurReader, Freq.Reader freqReader)
        {
            _fuelInstHhCurReader = fuelInstHhCurReader;
            _freqReader = freqReader;
        }

        private readonly FuelInstHhCur.Reader _fuelInstHhCurReader;
        private readonly Freq.Reader _freqReader;

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
            var tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];
            var updateTime = latestData.Update.InZone(tzLondon);
            var fuelInstHhCurData = new IndexModel.FuelInstHhCurModel(rows, total, co2KgPerKwh, updateTime);
            var freqCount = (int)await _freqReader.CountAsync();
            var freqData = await _freqReader.ReadAsync(freqCount - 1, freqCount);
            var freqLatest = await freqData.FirstOrDefault();
            var frequencyData = new IndexModel.FrequencyModel(freqLatest.Frequency, freqLatest.Update.InZone(tzLondon));
            var model = new IndexModel(fuelInstHhCurData, frequencyData);
            return View(model);
        }

        public IActionResult Error()
        {
            return View();
        }
    }
}

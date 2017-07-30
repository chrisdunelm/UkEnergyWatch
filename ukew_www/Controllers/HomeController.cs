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

            public IndexModel(IEnumerable<Row> rows, Row total, double co2KgPerKwh, ZonedDateTime updateTime)
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

        public HomeController(FuelInstHhCur.Reader fuelInstHhCurReader)
        {
            _fuelInstHhCurReader = fuelInstHhCurReader;
        }

        private readonly FuelInstHhCur.Reader _fuelInstHhCurReader;

        public async Task<IActionResult> Index()
        {
            var count = (int)await _fuelInstHhCurReader.CountAsync();
            var data = await _fuelInstHhCurReader.ReadAsync(count - 1, count);
            var latestData = await data.FirstOrDefault();
            var rows = new []
            {
                new IndexModel.Row("Combined Cycle Gas Turbine", latestData.Ccgt, latestData.Ccgt.CalculateCo2(1.02006335797254e-7)),
                new IndexModel.Row("Open Cycle Gas Turbine", latestData.Ocgt, latestData.Ocgt.CalculateCo2(0)), // TODO: CO2
                new IndexModel.Row("Oil", latestData.Oil, latestData.Oil.CalculateCo2(0)), // TODO: CO2
                new IndexModel.Row("Coal", latestData.Coal, latestData.Coal.CalculateCo2(2.71604938271605e-7)),
                new IndexModel.Row("Nuclear", latestData.Nuclear, MassFlow.Zero),
                new IndexModel.Row("Wind", latestData.Wind, MassFlow.Zero),
                new IndexModel.Row("Pumped Storage Hydro", latestData.Ps, MassFlow.Zero),
                new IndexModel.Row("Non Pumped Storage Hydro", latestData.Npshyd, MassFlow.Zero),
                new IndexModel.Row("Other", latestData.Other),
                new IndexModel.Row("Interconnect - France", latestData.IntFr),
                new IndexModel.Row("Interconnect - Ireland (Moyle)", latestData.IntIrl),
                new IndexModel.Row("Interconnect - Netherlands", latestData.IntNed),
                new IndexModel.Row("Interconnect - Ireland (East-West)", latestData.IntEw),
            };
            var totalCo2 = MassFlow.FromKilogramsPerSecond(rows.Sum(x => x.Co2?.KilogramsPerSecond)).Value;
            var total = new IndexModel.Row("Total", latestData.Total, totalCo2);
            var co2KgPerKwh = totalCo2.KilogramsPerHour / latestData.Total.Kilowatts;
            var updateTime = latestData.Update.InZone(DateTimeZoneProviders.Tzdb["Europe/London"]);
            updateTime.ToString("F", null);
            var model = new IndexModel(rows, total, co2KgPerKwh, updateTime);
            return View(model);
        }

        public IActionResult Error()
        {
            return View();
        }
    }
}

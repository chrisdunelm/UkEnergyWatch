using System;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Ukew.Elexon;
using Ukew.MemDb;
using Ukew.Utils.Tasks;
using ukew_www_blazor.Shared;

namespace ukew_www_blazor.Server.Controllers
{
    [Route("api/[controller]")]
    public class DataController : Controller
    {
        public DataController(
            ITaskHelper taskHelper,
            Db<FuelInstHhCur.Data> fuelInstHhCur)
        {
            _taskHelper = taskHelper;
            _fuelInstHhCur = fuelInstHhCur;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly Db<FuelInstHhCur.Data> _fuelInstHhCur;

        [HttpPost("[action]")]
        public Task<byte[]> Get([FromBody] DataRequest[] requests)
        {
            foreach (var request in requests)
            {
                GetTimeSeries(request);
            }
            return null;
        }

        private void GetTimeSeries(DataRequest request)
        {
            switch (request.TimeSeriesId)
            {
                case TimeSeries.ElectricityFuelInstOcgt:
                    break;
                default:
                    throw new NotImplementedException($"Cannot handle time-series: '{request.TimeSeriesId}'");
            }
        }
    }
}

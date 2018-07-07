// using System.Collections.Generic;
// using System.Threading.Tasks;
// using Microsoft.AspNetCore.Mvc;
// using Ukew.Elexon;
// using Ukew.MemDb;
// using Ukew.Utils.Tasks;
// using ukew_www_blazor.Shared;

// namespace ukew_www_blazor.Server.Controllers
// {
//     [Route("api/[controller]")]
//     public class ElexonDataController : Controller
//     {
//         public ElexonDataController(
//             ITaskHelper taskHelper,
//             Db<FuelInstHhCur.Data> fuelInstHhCur)
//         {
//             _taskHelper = taskHelper;
//             _fuelInstHhCur = fuelInstHhCur;
//         }

//         private ITaskHelper _taskHelper;
//         private Db<FuelInstHhCur.Data> _fuelInstHhCur;

//         [HttpGet("[action]")]
//         public async Task<FuelInstHhCurData> FuelInstHhCur()
//         {
//             await _fuelInstHhCur.InitialiseTask.ConfigureAwait(_taskHelper);
//             var data = _fuelInstHhCur.Last();
//             return new FuelInstHhCurData
//             {
//                 CcgtMw = (int)data.Ccgt.Megawatts,
//                 OcgtMw = (int)data.Ocgt.Megawatts,
//                 OilMw = (int)data.Oil.Megawatts,
//                 CoalMw = (int)data.Coal.Megawatts,
//                 WindMw = (int)data.Wind.Megawatts,
//             };
//         }
//     }
// }

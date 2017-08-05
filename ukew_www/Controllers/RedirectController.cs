using Microsoft.AspNetCore.Mvc;

namespace ukew_www.Controllers
{
    public class RedirectController : Controller
    {
        [HttpGet("/Electricity")]
        [HttpGet("/Electricity/Realtime")]
        public IActionResult RedirectToIndex()
        {
            return this.Redirect("/");
        }
    }
}

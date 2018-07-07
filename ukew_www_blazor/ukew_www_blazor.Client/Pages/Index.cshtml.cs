using Blazor.Extensions;
using Microsoft.AspNetCore.Blazor.Components;
using System;

namespace ukew_www_blazor.Client
{
    public class IndexModel : BlazorComponent
    {
        protected BECanvasComponent _canvas;
        private Canvas2dContext _ctx;

        //[Inject]
        //protected HttpClient Http { get; set; }

        protected override void OnAfterRender()
        {
            _ctx = _canvas.CreateCanvas2d();
            Console.WriteLine("Canvas happening");
            _ctx.FillStyle = "green";
            _ctx.FillRect(10, 10, 50, 50);
        }
    }
}

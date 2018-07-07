using System;
using Microsoft.AspNetCore.Razor.TagHelpers;
using Ukew.Elexon;
using Ukew.Utils;
using UnitsNet;
using UnitsNet.Units;
using ukew_www_blazor.Server.Utils;

namespace ukew_www_blazor.Server.TagHelpers
{
    public class FuelInstTagHelper : TagHelper
    {
        public int Index { get; set; }
        public string Name { get; set; }
        public Power Power { get; set; }
        public FuelInstHhCur.Data All { get; set; }
        public MassFlow? Co2 { get; set; } = null;

        public override void Process(TagHelperContext context, TagHelperOutput output)
        {
            var rowClass = $"row{Index & 1}";
            var percent = ((Power / All.Total) * 100.0).ToString("N1") + " %";
            var co2 = Co2?.DisplayKg_s();
            var html = $@"
<tr class=""{rowClass} highlight"">
    <td>{Name}</td>
    <td><span class=""mw"">{Power.DisplayMW()}</span><span class=""percent"">{percent}</span></td>
    <td class=""{(co2 == null ? "unknown" : "")}"">{co2 ?? "unknown"}</td>
</tr>";
            output.Content.SetHtmlContent(html);
            output.TagName = null;
        }
    }
}

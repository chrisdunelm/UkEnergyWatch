using System.Collections.Generic;
using System.Linq;
using System.Text;
using Microsoft.AspNetCore.Razor.TagHelpers;
using NodaTime;
using Ukew.Elexon;
using ukew_www_blazor.Server.Utils;
using UnitsNet;

namespace ukew_www_blazor.Server.TagHelpers
{
    public class PowerTypeHeaderTagHelper : TagHelper
    {
        private static DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

        public EicIds.FuelType FuelType { get; set; }
        public Instant FpnTime { get; set; }
        public Instant B1610Time { get; set; }

        public override void Process(TagHelperContext context, TagHelperOutput output)
        {
            var fpnTime = FpnTime.InZone(s_tzLondon).ToString("dd MMMM yyyy HH:mm:ss", null);
            var b1610Time = B1610Time.InZone(s_tzLondon).ToString("dd MMMM yyyy HH:mm:ss", null);
            var html = $@"
<tr class=""header x reveal_fpn"">
    <td>Power Station - {FuelType}</td>
    <td>
        <div>Predicted Power <label class=""button right"" for=""box_b1610"">Show Actual</label></div>
        <div class=""smallupdate"">{fpnTime}</div>
    </td>
    <td>CO<sub>2</sub> emissions</td>
</tr>
<tr class=""header x reveal_b1610"">
    <td>Power Station - {FuelType}</td>
    <td>
        <div>Actual Power <label class=""button right"" for=""box_fpn"">Show Predicted</label></div>
        <div class=""smallupdate"">{b1610Time}</div>
    </td>
    <td>CO<sub>2</sub> emissions</td>
</tr>";
            output.Content.SetHtmlContent(html);
            output.TagName = null;
        }
    }

    public class PowerStationTagHelper : TagHelper
    {
        public int Index { get; set; }
        public EicIds.PowerStation Ps { get; set; }
        public Dictionary<string, (Power power, Instant update)> Fpn { get; set; }
        public Dictionary<string, (Power power, Instant update)> B1610 { get; set; }

        private (string name, T? data) Get<T>(Dictionary<string, T> d, string key) where T : struct =>
            (key, d.TryGetValue(key, out var v) ? v : (T?)null);

        public override void Process(TagHelperContext context, TagHelperOutput output)
        {
            var fpns = Ps.GenerationUnitRegisteredResourceNames.Select(x => Get(Fpn, x)).ToList();
            var fpnTotal = fpns.Aggregate(Power.Zero, (a, x) => a + (x.data?.power ?? Power.Zero));
            var fpnAny = fpns.Any(x => x.name != null);
            var b1610s = Ps.GenerationUnitRegisteredResourceNames.Select(x => Get(B1610, x)).ToList();
            var b1610Total = b1610s.Aggregate(Power.Zero, (a, x) => a + (x.data?.power ?? Power.Zero));
            var b1610Any = b1610s.Any(x => x.name != null);
            var html = $@"
<tr class=""row{Index & 1} x reveal_fpn highlight"">
    <td><label class=""revealer"" for=""box{Ps.RegisteredResourceName}"">
        {Ps.AssetName} ({Ps.RegisteredResourceName})
    </label></td>
    <td class=""{(fpnAny ? "" : "unknown")}"">
        {(fpnAny ? fpnTotal.DisplayMW() : "unknown")}
        <span class=""maxpower"">
            installed: {Ps.InstalledCapacity?.DisplayMW() ?? "unknown"}
        </span>
    </td>
    <td class=""unknown"">
        -
    </td>
</tr>
<tr class=""row{Index & 1} x reveal_b1610 highlight"">
    <td><label class=""revealer"" for=""box{Ps.RegisteredResourceName}"">
        {Ps.AssetName} ({Ps.RegisteredResourceName})
    </label></td>
    <td class=""{(b1610Any ? "" : "unknown")}"">
        {(b1610Any ? b1610Total.DisplayMW() : "unknown")}
        <span class=""maxpower"">
            installed: {Ps.InstalledCapacity?.DisplayMW() ?? "unknown"}
        </span>
    </td>
    <td class=""unknown"">
        -
    </td>
</tr>";
            var fpnHtml = fpns.Select(fpn =>
            {
                var genUnit = EicIds.GenerationUnitsByResourceName[fpn.name];
                return $@"
<tr class=""row{Index & 1} x reveal{Ps.CssId()}_fpn"">
    <td class=""genunit"">{fpn.name}</td>
    <td class=""{(fpn.data != null ? "" : "unknown")}"">
        {fpn.data?.power.DisplayMW() ?? "unknown"}
        <span class=""maxpower"">
            nominal: {genUnit.MaxCapacity?.DisplayMW() ?? "unknown"}
        </span>
    </td>
    <td class=""unknown"">
        -
    </td>
</tr>";
            }).Aggregate(new StringBuilder(256), (a, x) => a.Append(x));
            var b1610Html = b1610s.Select(b1610 =>
            {
                var genUnit = EicIds.GenerationUnitsByResourceName[b1610.name];
                return $@"
<tr class=""row{Index & 1} x reveal{Ps.CssId()}_b1610"">
    <td class=""genunit"">{b1610.name}</td>
    <td class=""{(b1610.data != null ? "" : "unknown")}"">
        {b1610.data?.power.DisplayMW() ?? "unknown"}
        <span class=""maxpower"">
            nominal: {genUnit.MaxCapacity?.DisplayMW() ?? "unknown"}
        </span>
    </td>
    <td class=""unknown"">
        -
    </td>
</tr>";
            }).Aggregate(new StringBuilder(256), (a, x) => a.Append(x));
            output.Content.SetHtmlContent(html + fpnHtml + b1610Html);
            output.TagName = null;
        }
    }
}

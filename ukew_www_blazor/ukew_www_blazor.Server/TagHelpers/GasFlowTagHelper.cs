using Microsoft.AspNetCore.Razor.TagHelpers;
using ukew_www_blazor.Server.Utils;
using UnitsNet;

namespace ukew_www_blazor.Server.TagHelpers
{
    public class GasFlowTagHelper : TagHelper
    {
        public int Index { get; set; } = -1;
        public string Name { get; set; }
        public Flow Flow { get; set; }

        public override void Process(TagHelperContext context, TagHelperOutput output)
        {
            var rowClass = Index >= 0 ? $"row{Index & 1}" : "footer";
            var highlightClass = Index >= 0 ? "highlight" : "";
            var html = $@"
<tr class=""{rowClass} {highlightClass}"">
    <td>{Name}</td>
    <td>
        <span class=""mw"">{Flow.DisplayM3_s()}</span>
        <span class=""percent"">({Flow.DisplayMm3_day()})</span>
    </td>
    <td>-</td>
</tr>
            ";
            output.Content.SetHtmlContent(html);
            output.TagName = null;
        }
    }
}

using System.Threading.Tasks;
using Microsoft.AspNetCore.Razor.TagHelpers;
using NodaTime;

namespace ukew_www_blazor.Server.TagHelpers
{
    public class UpdateTimeTagHelper : TagHelper
    {
        private static DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

        public Instant Instant { get; set; }

        public override void Process(TagHelperContext context, TagHelperOutput output)
        {
            var timeString = Instant.InZone(s_tzLondon).ToString("dd MMMM yyyy HH:mm:ss", null);
            var content = $"Updated: {timeString} (UK local time)";
            output.TagName = null;
            output.Content.SetContent(content);
        }
    }
}

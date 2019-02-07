using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Markdig;
using NodaTime;
using NodaTime.Text;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace ukew_www_blazor.Server.Blogging
{
    public class Post
    {
        public static async Task<Post> CreateAsync(ITaskHelper th, ITime time,Stream content)
        {
            var bytes = new List<byte>(4096);
            var buffer = new byte[4096];
            while (true)
            {
                int read = await content.ReadAsync(buffer, 0, buffer.Length).ConfigureAwait(th);
                if (read == 0)
                {
                    break;
                }
                bytes.AddRange(buffer.Take(read));
            }
            var md = Encoding.UTF8.GetString(bytes.ToArray());
            var preambleParts = md.Split("---", 3);
            var preamble = preambleParts[1].Trim().Split('\n').Select(x => x.Split(':', 2))
                .ToDictionary(x => x[0].Trim(), x => x[1].Trim());
            var pipeline = new MarkdownPipelineBuilder().UseAdvancedExtensions().Build();
            var html = Markdown.ToHtml(preambleParts[2], pipeline);
            return new Post(time, preamble, html);
        }

        private Post(ITime time, IReadOnlyDictionary<string, string> preamble, string html)
        {
            _time = time;
            Id = preamble["id"];
            var publishTimeUtc = preamble.TryGetValue("publish-time-utc", out var pt) ?
                InstantPattern.ExtendedIso.Parse(pt).Value : Instant.FromUtc(2100, 1, 1, 0, 0);
            Title = preamble["title"];
            Html = html;
        }

        private readonly ITime _time;

        public string Id { get; }
        public Instant PublishTime { get; }
        public string Title { get; }

        public bool IsPublished() => _time.GetCurrentInstant() >= PublishTime;

        public string Html { get; }
    }
}

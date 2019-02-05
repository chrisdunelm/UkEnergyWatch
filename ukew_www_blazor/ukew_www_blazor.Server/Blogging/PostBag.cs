using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace ukew_www_blazor.Server.Blogging
{
    public class PostBag
    {
        public  PostBag(ITaskHelper th, ITime time, CmdLineOptions options)
        {
            _th = th;
            _time = time;
            _initTcs = new TaskCompletionSource<int>();
            th.Run(() => InitAsync(new SystemDirectory(th, options.BlogRootDirectory)));
        }

        private readonly ITaskHelper _th;
        private readonly ITime _time;
        private readonly TaskCompletionSource<int> _initTcs;
        
        private readonly object _lock = new object();
        private IReadOnlyDictionary<string, Post> _posts;

        private async Task InitAsync(IDirectory rootDir)
        {
            while (true)
            {
                var awaitTask = rootDir.AwaitChange("*.md", Duration.FromHours(1));
                var files = await rootDir.ListFilesAsync().ConfigureAwait(_th);
                var postList = new List<Post>();
                foreach (var file in files.Where(x => x.Id.EndsWith(".md")))
                {
                    try
                    {
                        using (var content = await rootDir.ReadAsync(file.Id).ConfigureAwait(_th))
                        {
                            var post = await Post.CreateAsync(_th, _time, content).ConfigureAwait(_th);
                            postList.Add(post);
                        }
                    }
                    catch (Exception e)
                    {
                        // TODO: Use logging.
                        Console.WriteLine();
                        Console.WriteLine($"Error load blog post: '{file.Id}'");
                        Console.WriteLine(e);
                        Console.WriteLine();
                    }
                }
                var posts = postList.Where(x => x != null).ToDictionary(x => x.Id);
                Interlocked.Exchange(ref _posts, posts);
                _initTcs.TrySetResult(0);
                await awaitTask.ConfigureAwait(_th);
            }
        }

        /// <summary>
        /// Returns all published posts, order by publish time, most recent first.
        /// </summary>
        public async Task<IEnumerable<Post>> GetAllPostsAsync(bool includeUnpublished)
        {
            await _initTcs.Task.ConfigureAwait(_th);
            var posts = Interlocked.CompareExchange(ref _posts, null, null);
            return posts.Values.Where(x => includeUnpublished || x.IsPublished())
                .OrderByDescending(x => x.PublishTime);
        }

        public async Task<Post> GetPostAsync(string id, bool includeUnpublished)
        {
            await _initTcs.Task.ConfigureAwait(_th);
            var posts = Interlocked.CompareExchange(ref _posts, null, null);
            var post = posts.GetValueOrDefault(id);
            return post != null && (includeUnpublished || post.IsPublished()) ? post : null;
        }
    }
}

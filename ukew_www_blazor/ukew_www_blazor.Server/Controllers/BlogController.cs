using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Ukew.Storage;
using System.Linq;
using System.Collections.Generic;
using System;
using ukew_www_blazor.Server.Blogging;
using Ukew.Utils.Tasks;

namespace ukew_www_blazor.Server.Controllers
{
    public class BlogController : Controller
    {
        public BlogController(ITaskHelper th, PostBag postBag)
        {
            _th = th;
            _postBag = postBag;
        }

        private readonly ITaskHelper _th;
        private readonly PostBag _postBag;

        [HttpGet("/blog/{*path}")]
        public async Task<IActionResult> Blog()
        {
            var path = Request.Path;
            var postId = path.Value.Substring(Math.Min("/blog/".Length, path.Value.Length));
            var post = await _postBag.GetPostAsync(postId, false).ConfigureAwait(_th);
            if (post != null)
            {
                return View("BlogPost", post);
            }
            else
            {
                var allPosts = await _postBag.GetAllPostsAsync(false).ConfigureAwait(_th);
                var posts = allPosts.Take(10);
                return View("BlogIndex", posts);
            }
        }
    }
}

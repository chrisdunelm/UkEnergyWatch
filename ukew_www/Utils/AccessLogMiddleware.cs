using System;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Ukew.Logging;
using Ukew.Storage;
using Ukew.Utils.Tasks;
using ukew_www;

namespace Ukew.Utils
{
    public class AccessLogMiddleware
    {
        public const string BaseName = "access";

        public AccessLogMiddleware(IDirectory dir)
        {
            _time = SystemTime.Instance;
            _appender = new RollingFileAppender(dir, BaseName, SystemTaskHelper.Instance);
        }

        private readonly ITime _time;
        private readonly ITextAppender _appender;

        private string Quote(string s) => $"\"{s.Replace("\\", "\\\\").Replace("\"", "\\\"")}\"";

        public async Task Invoke(HttpContext context, Func<Task> next)
        {
            var requestInstant = _time.GetCurrentInstant();
            await next();
            var responseInstant = _time.GetCurrentInstant();
            var request = context.Request;
            var response = context.Response;
            var remoteIp = request.Headers["X-Forwarded-For"].LastOrDefault() ?? context.Connection.RemoteIpAddress.ToString();
            var responseDelay = responseInstant - requestInstant;
            var line0 = $"{requestInstant.ToString("uuuu-MM-dd HH:mm:ss.fff", null)}, {(int)responseDelay.TotalMilliseconds}";
            var line1 = $"{Quote(request.Method)}, {Quote(request.Path)}, {Quote(request.QueryString.ToString())}, {Quote(remoteIp)}";
            var line2 = $"{Quote(string.Join(", ", request.Headers["User-Agent"]))}, {response.StatusCode}, {request.ContentLength ?? -1}, {response.ContentLength ?? -1}";
            await _appender.AppendLineAsync($"2, {line0}, {line1}, {line2}");
            // Version,
            // Request time, response delay (ms), req method, req path, req querystring,
            // remoteip:port, user agent, response status code, request content length, response content length
        }
    }

    public static class AccessLogMiddlewareExtensions
    {
        public static IApplicationBuilder UseAccessLog(this IApplicationBuilder app, IDirectory dir)
        {
            var accessLog = new AccessLogMiddleware(dir);
            return app.Use(accessLog.Invoke);
        }
    }
}

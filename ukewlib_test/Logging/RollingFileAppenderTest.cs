using System;
using System.IO;
using System.Linq;
using System.Text;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Logging
{
    public class RollingFileAppenderTest
    {
        [Fact]
        public void AppendLines()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var nl = Environment.NewLine;
                var expected = $"one{nl}two{nl}";
                var dir = new FakeDirectory();
                var appender = new RollingFileAppender(dir, "abc", th);
                await appender.AppendLineAsync("one").ConfigureAwait(th);
                await appender.AppendLineAsync("two").ConfigureAwait(th);
                var files = (await dir.ListFilesAsync().ConfigureAwait(th)).ToList();
                Assert.Single(files);
                Assert.Equal(expected.Length, files[0].Length);
                var stream = await dir.ReadAsync(files[0].Id).ConfigureAwait(th);
                var ms = new MemoryStream();
                await stream.CopyToAsync(ms).ConfigureAwait(th);
                Assert.Equal(expected, Encoding.UTF8.GetString(ms.ToArray()));
            });
        }

        // TODO: Test rollingness
    }
}

using NodaTime;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Storage
{
    public class DirectoryExtensionsTest
    {
        [Fact]
        public void WatchTimeout() => TimeRunner.Run(async (NodaTime, th) =>
        {
            var dir = new FakeDirectory(th);
            var timedOut0 = await dir.AwaitChange("", Duration.FromSeconds(10)).ConfigureAwait(th);
            Assert.True(timedOut0);
            var timedOut1Task = dir.AwaitChange("", Duration.FromSeconds(10));
            await th.Delay(Duration.FromSeconds(5));
            await dir.AppendAsync("afile", new byte[] { 0 }).ConfigureAwait(th);
            var timedOut1 = await timedOut1Task.ConfigureAwait(th);
            Assert.False(timedOut1);
        });
    }
}
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Applications
{
    public class FetchPhyBmDataTest
    {
        [Fact]
        public void Fetch()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var downloader = new FakeElexonDownloader();
                var dir = new FakeDirectory();
                var fetcher = new FetchPhyBmData(th, downloader, dir, time);

                var cts = new CancellationTokenSource();
                Task unused = th.Run(async () =>
                {
                    await th.Delay(NodaTime.Duration.FromMinutes(60)).ConfigureAwait(th);
                    cts.Cancel();
                });

                await fetcher.Start(false, cts.Token).ConfigureAwaitHideCancel(th);

                var reader = new PhyBmData.FpnReader(th, dir);
                Assert.Equal(1031 + 1023, await reader.CountAsync().ConfigureAwait(th));
                var cldcwIdHash = EicIds.Hash("CLDCW-1");
                var cldcwTask = (await reader.ReadAsync().ConfigureAwait(th)).Where(x => x.ResourceNameHash == cldcwIdHash);
                var cldcw = await cldcwTask.ToList().ConfigureAwait(th);
                Assert.Equal(4, cldcw.Count);
                Assert.Equal(new [] { 13.0, 11.0, 11.0, 9.0 }, cldcw.Select(x => x.LevelTo.Megawatts));
            }, startInstant: NodaTime.Instant.FromUtc(2017, 8, 5, 22, 29));
        }
    }
}

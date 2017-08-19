using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Applications
{
    public class FetchB1610Test
    {
        [Fact]
        public void Fetch()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var downloader = new FakeElexonDownloader();
                var dir = new FakeDirectory();
                var fetcher = new FetchB1610(th, downloader, dir, time);

                var cts = new CancellationTokenSource();
                Task unused = th.Run(async () =>
                {
                    await th.Delay(NodaTime.Duration.FromMinutes(5)).ConfigureAwait(th);
                    cts.Cancel();
                });

                await fetcher.Start(cts.Token).ConfigureAwaitHideCancel(th);

                var reader = new B1610.Reader(th, dir);
                Assert.Equal(100 + 103, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(new B1610.Data("GRAI-6", Instant.FromUtc(2017, 7, 31, 23, 0), Power.FromKilowatts(226_200)),
                    await (await reader.ReadAsync(0, 1)).First());
                Assert.Equal(new B1610.Data("GRGBW-1", Instant.FromUtc(2017, 7, 31, 23, 30), Power.FromKilowatts(43_390)),
                    await (await reader.ReadAsync(100 + 103 - 1, 100 + 103)).First());
            }, startInstant: NodaTime.Instant.FromUtc(2017, 8, 10, 22, 30));
        }
    }
}

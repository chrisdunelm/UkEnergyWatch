using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Applications
{
    public class FetchFreqTest
    {
        [Fact]
        public void Fetch()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var downloader = new FakeElexonDownloader();
                var dir = new FakeDirectory();
                var fetcher = new FetchFreq(th, downloader, dir, time);

                var cts = new CancellationTokenSource();
                Task unused = th.Run(async () =>
                {
                    await th.Delay(NodaTime.Duration.FromMinutes(3)).ConfigureAwait(th);
                    cts.Cancel();
                });
                await fetcher.Start(false, cts.Token).ConfigureAwaitHideCancel(th);

                var reader = new Freq.Reader(th, dir);
                Assert.Equal((60 + 2) * 4, await reader.CountAsync().ConfigureAwait(th));
                var data = await (await reader.ReadAsync().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Equal((60 + 2) * 4, data.Count);
                Assert.Equal(new Freq.Data(NodaTime.Instant.FromUtc(2017, 8, 2, 11, 0, 15), Frequency.FromHertz(49.910)), data.First());
                Assert.Equal(new Freq.Data(NodaTime.Instant.FromUtc(2017, 8, 2, 12, 2, 0), Frequency.FromHertz(50.040)), data.Last());
            }, startInstant: NodaTime.Instant.FromUtc(2017, 8, 2, 12, 0));
        }
    }
}

using System.Linq;
using NodaTime;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Elexon
{
    public class FreqTest
    {
        [Fact]
        public void Download()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var freq = new Freq(th, new FakeElexonDownloader());
                var data = await freq.GetAsync(Instant.FromUtc(2017, 8, 2, 10, 30), Instant.FromUtc(2017, 8, 2, 11, 30)).ConfigureAwait(th);
                Assert.Equal(60 * 4 + 1, data.Count);
                Assert.Equal(new Freq.Data(Instant.FromUtc(2017, 8, 2, 10, 30), Frequency.FromHertz(50.014)), data.First());
                Assert.Equal(new Freq.Data(Instant.FromUtc(2017, 8, 2, 11, 30), Frequency.FromHertz(50.165)), data.Last());
            });
        }

        [Fact]
        public void DownloadEmpty()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var freq = new Freq(th, new FakeElexonDownloader());
                var data = await freq.GetAsync(Instant.FromUtc(2018, 1, 1, 0, 0), Instant.FromUtc(2018, 1, 1, 0, 1)).ConfigureAwait(th);
                Assert.Empty(data);
            });
        }

        [Fact]
        public void StoreAndLoad()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory(th);
                var freq = new Freq(th, new FakeElexonDownloader());
                var data = (await freq.GetAsync(Instant.FromUtc(2017, 8, 2, 10, 30), Instant.FromUtc(2017, 8, 2, 11, 30)).ConfigureAwait(th));
                var writer = new Freq.Writer(th, dir);
                var reader = new Freq.Reader(th, dir);
                Assert.Equal(0, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(0, await (await reader.ReadAsync().ConfigureAwait(th)).Count().ConfigureAwait(th));
                await writer.AppendAsync(data).ConfigureAwait(th);
                Assert.Equal(60 * 4 + 1, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(data[0], await (await reader.ReadAsync(0, 1).ConfigureAwait(th)).Single().ConfigureAwait(th));
                Assert.Equal(data[10], await (await reader.ReadAsync(10, 11).ConfigureAwait(th)).Single().ConfigureAwait(th));
                Assert.Equal(data.Take(10), await (await reader.ReadAsync(0, 10).ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Equal(data.Skip(50).Take(10), await (await reader.ReadAsync(50, 60).ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Equal(data, await (await reader.ReadAsync().ConfigureAwait(th)).ToList().ConfigureAwait(th));
            });
        }
    }
}

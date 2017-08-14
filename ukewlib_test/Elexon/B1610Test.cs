using System.Linq;
using NodaTime;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Elexon
{
    public class B1610Test
    {
        [Fact]
        public void Download()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var freq = new B1610(th, new FakeElexonDownloader());
                var data = await freq.GetAsync(new LocalDate(2017, 8, 1), 1).ConfigureAwait(th);
                Assert.Equal(100, data.Count);
                Assert.Equal(new B1610.Data("GRAI-6", Instant.FromUtc(2017, 7, 31, 23, 0), Power.FromKilowatts(226_200)), data.First());
                Assert.Equal(new B1610.Data("GRGBW-1", Instant.FromUtc(2017, 7, 31, 23, 0), Power.FromKilowatts(50_140)), data.Last());
            });
        }
 
        [Fact]
        public void DownloadEmpty()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var freq = new B1610(th, new FakeElexonDownloader());
                var data = await freq.GetAsync(new LocalDate(2018, 1, 1), 1).ConfigureAwait(th);
                Assert.Empty(data);
            });
        }

        [Fact]
        public void StoreAndLoad()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory();
                var freq = new B1610(th, new FakeElexonDownloader());
                var data = await freq.GetAsync(new LocalDate(2017, 8, 1), 1).ConfigureAwait(th);
                var writer = new B1610.Writer(th, dir);
                var reader = new B1610.Reader(th, dir);
                Assert.Equal(0, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(0, await (await reader.ReadAsync().ConfigureAwait(th)).Count().ConfigureAwait(th));
                await writer.AppendAsync(data).ConfigureAwait(th);
                Assert.Equal(100, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(data[0], await (await reader.ReadAsync(0, 1).ConfigureAwait(th)).Single().ConfigureAwait(th));
                Assert.Equal(data[10], await (await reader.ReadAsync(10, 11).ConfigureAwait(th)).Single().ConfigureAwait(th));
                Assert.Equal(data.Take(10), await (await reader.ReadAsync(0, 10).ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Equal(data.Skip(50).Take(10), await (await reader.ReadAsync(50, 60).ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Equal(data, await (await reader.ReadAsync().ConfigureAwait(th)).ToList().ConfigureAwait(th));
            });
        }
    }
}

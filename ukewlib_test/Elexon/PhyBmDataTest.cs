using System.Collections.Immutable;
using System.Linq;
using NodaTime;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Elexon
{
    public class PhyBmDataTest
    {
        [Fact]
        public void Download()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var phy = new PhyBmData(th, new FakeElexonDownloader());
                var data = await phy.GetAsync(new LocalDate(2017, 8, 6), 1).ConfigureAwait(th);
                Assert.Equal(1031, data.Count);
                Assert.Equal(Power.FromMegawatts(-225), data[0].LevelFrom);
                var fpn0 = new PhyBmData.FpnData("EAS-BGS01",
                    Instant.FromUtc(2017, 8, 5, 23, 0), Power.FromMegawatts(-225),
                    Instant.FromUtc(2017, 8, 5, 23, 30), Power.FromMegawatts(-225));
                Assert.Equal(fpn0, data[0]);
                var fpnLast = new PhyBmData.FpnData("RHAMD-1",
                    Instant.FromUtc(2017, 8, 5, 23, 0), Power.FromMegawatts(-2),
                    Instant.FromUtc(2017, 8, 5, 23, 30), Power.FromMegawatts(-2));
                Assert.Equal(fpnLast, data.Last());
                var whilw1 = data.Where(x => x.ResourceNameHash == EicIds.Hash("WHILW-1")).ToImmutableList();
                Assert.Equal(2, whilw1.Count);
                var whilw1Fpn0 = new PhyBmData.FpnData("WHILW-1",
                    Instant.FromUtc(2017, 8, 5, 23, 0), Power.FromMegawatts(25),
                    Instant.FromUtc(2017, 8, 5, 23, 1), Power.FromMegawatts(14));
                Assert.Equal(whilw1Fpn0, whilw1[0]);
                var whilw1Fpn1 = new PhyBmData.FpnData("WHILW-1",
                    Instant.FromUtc(2017, 8, 5, 23, 1), Power.FromMegawatts(14),
                    Instant.FromUtc(2017, 8, 5, 23, 30), Power.FromMegawatts(14));
                Assert.Equal(whilw1Fpn1, whilw1[1]);
            });
        }

        [Fact]
        public void DownloadEmpty()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var phy = new PhyBmData(th, new FakeElexonDownloader());
                var data = await phy.GetAsync(new LocalDate(2018, 1, 1), 1).ConfigureAwait(th);
                Assert.Empty(data);
            });
        }

        [Fact]
        public void StoreAndLoad()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory(th);
                var phy = new PhyBmData(th, new FakeElexonDownloader());
                var data = await phy.GetAsync(new LocalDate(2017, 8, 6), 1).ConfigureAwait(th);
                var writer = new PhyBmData.FpnWriter(th, dir);
                var reader = new PhyBmData.FpnReader(th, dir);
                Assert.Equal(0, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(0, await (await reader.ReadAsync().ConfigureAwait(th)).Count().ConfigureAwait(th));
                await writer.AppendAsync(data).ConfigureAwait(th);
                Assert.Equal(1031, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(data[0], await (await reader.ReadAsync(0, 1).ConfigureAwait(th)).Single().ConfigureAwait(th));
                Assert.Equal(data[10], await (await reader.ReadAsync(10, 11).ConfigureAwait(th)).Single().ConfigureAwait(th));
                Assert.Equal(data.Take(10), await (await reader.ReadAsync(0, 10).ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Equal(data.Skip(50).Take(10), await (await reader.ReadAsync(50, 60).ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Equal(data, await (await reader.ReadAsync().ConfigureAwait(th)).ToList().ConfigureAwait(th));
            });
        }
    }
}

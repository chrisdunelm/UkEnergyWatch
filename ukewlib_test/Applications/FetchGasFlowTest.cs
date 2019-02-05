using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;
using NodaTime;
using Ukew.NationalGrid;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Applications
{
    public class FetchGasFlowTest
    {
        [Fact]
        public void Fetch()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var entries = new Dictionary<(string uri, string soapAction, string soapBody), XElement>
                {
                    { ("http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx",
                        "http://www.NationalGrid.com/EDP/UI/GetLatestPublicationTime",
                        "<GetLatestPublicationTime xmlns=\"http://www.NationalGrid.com/EDP/UI/\"/>"),
                        Soap.LoadBody("GetLatestPublicationTime1") },
                    { ("http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx",
                        "http://www.NationalGrid.com/EDP/UI/GetInstantaneousFlowData",
                        "<GetInstantaneousFlowData xmlns=\"http://www.NationalGrid.com/EDP/UI/\" />"),
                        Soap.LoadBody("GetInstantaneousFlowData1") }
                };
                var downloader = new FakeSoapDownloader(entries);
                var dir = new FakeDirectory(th);
                var fetcher = new FetchGasFlow(th, downloader, dir, time);

                var cts = new CancellationTokenSource();
                Task unused = th.Run(async () =>
                {
                    await th.Delay(NodaTime.Duration.FromMinutes(60)).ConfigureAwait(th);
                    cts.Cancel();
                });
                await fetcher.Start(false, cts.Token).ConfigureAwaitHideCancel(th);

                var reader = new InstantaneousFlow.Reader(th, dir);
                var strings = new Strings(th, dir);
                Assert.Equal(258, await reader.CountAsync().ConfigureAwait(th));
                var data = await (await reader.ReadAsync().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Equal(258, data.Count);
                var nameIndex0 = await strings.AddOrGetIndexAsync("ALDBROUGH").ConfigureAwait(th);
                Assert.Equal(new InstantaneousFlow.Data(Instant.FromUtc(2017, 9, 15, 6, 24),
                    InstantaneousFlow.SupplyType.ZoneSupply, (ushort)nameIndex0, Flow.Zero), data.First());
                var nameIndex1 = await strings.AddOrGetIndexAsync("TOTAL SUPPLY").ConfigureAwait(th);
                Assert.Equal(new InstantaneousFlow.Data(Instant.FromUtc(2017, 9, 15, 6, 34),
                    InstantaneousFlow.SupplyType.TotalSupply, (ushort)nameIndex1, Flow.FromCubicMetersPerHour(190.03944 * 1e6 / 24)), data.Last());
            }, startInstant: NodaTime.Instant.FromUtc(2017, 9, 7, 16, 23));
        }
    }
}

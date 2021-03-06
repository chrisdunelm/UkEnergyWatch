using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Xml.Linq;
using NodaTime;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.NationalGrid
{
    public class InstantaneousFlowTest
    {
        [Fact]
        public void GetLatestPublicationTime()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var entries = new Dictionary<(string uri, string soapAction, string soapBody), XElement>
                {
                    { ("http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx",
                        "http://www.NationalGrid.com/EDP/UI/GetLatestPublicationTime",
                        "<GetLatestPublicationTime xmlns=\"http://www.NationalGrid.com/EDP/UI/\"/>"),
                        Soap.LoadBody("GetLatestPublicationTime1") }
                };
                var fakeSoapDownloader = new FakeSoapDownloader(entries);
                var flow = new InstantaneousFlow(th, fakeSoapDownloader);
                var instant = await flow.GetLatestPublicationTimeAsync().ConfigureAwait(th);
                Assert.Equal(Instant.FromUtc(2017, 9, 7, 16, 23, 0), instant);
            });
        }

        [Fact]
        public void GetInstantaneousFlowData()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var entries = new Dictionary<(string uri, string soapAction, string soapBody), XElement>
                {
                    { ("http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx",
                        "http://www.NationalGrid.com/EDP/UI/GetInstantaneousFlowData",
                        "<GetInstantaneousFlowData xmlns=\"http://www.NationalGrid.com/EDP/UI/\" />"),
                        Soap.LoadBody("GetInstantaneousFlowData1") }
                };
                var fakeSoapDownloader = new FakeSoapDownloader(entries);
                var flow = new InstantaneousFlow(th, fakeSoapDownloader);
                var stringsDir = new FakeDirectory(th);
                var strings = new Strings(th, stringsDir);
                var data = await (await flow.GetInstantaneousFlowDataAsync().ConfigureAwait(th)).WriteStringsAsync(th, strings).ConfigureAwait(th);
                Assert.Equal(258, data.Count);
                var nameIndex0 = await strings.AddOrGetIndexAsync("ALDBROUGH").ConfigureAwait(th);
                Assert.Equal(new InstantaneousFlow.Data(Instant.FromUtc(2017, 9, 15, 6, 24),
                    InstantaneousFlow.SupplyType.ZoneSupply, (ushort)nameIndex0, Flow.Zero), data.First());
                var nameIndex1 = await strings.AddOrGetIndexAsync("TOTAL SUPPLY").ConfigureAwait(th);
                Assert.Equal(new InstantaneousFlow.Data(Instant.FromUtc(2017, 9, 15, 6, 34),
                    InstantaneousFlow.SupplyType.TotalSupply, (ushort)nameIndex1, Flow.FromCubicMetersPerHour(190.03944 * 1e6 / 24)), data.Last());
            });
        }
    }
}

using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xunit;

namespace Ukew.Elexon
{
    public class BmHashTest
    {
        [Fact]
        public async Task CheckDups()
        {
            var downloader = new FakeElexonDownloader();
            var ps = new Dictionary<string, string>
            {
                { "SettlementDate", "2017_08_06" },
                { "SettlementPeriod", "1" },
            };
            var xDoc = await downloader.GetXmlAsync("PHYBMDATA", ps);
            var items = xDoc.Root.Element("responseBody").Element("responseList").Elements("item");
            Dictionary<uint, string> hashes = new Dictionary<uint, string>();
            bool failed = false;
            foreach (var item in items)
            {
                var bmUnitId = item.Element("bmUnitID").Value.Trim();
                var hash = BmUnitIds.Hash(bmUnitId);
                if (hashes.TryGetValue(hash, out var dupBmUnitId) && bmUnitId != dupBmUnitId)
                {
                    Console.WriteLine($"Duplicate hash ID 0x{hash:x8}. '{bmUnitId}', '{dupBmUnitId}'");
                    failed = true;
                }
                hashes[hash] = bmUnitId;
            }
            Assert.False(failed);
        }
    }
}

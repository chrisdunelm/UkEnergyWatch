using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew
{
    public class ElexonHhCurReport
    {
        [Fact]
        public async Task Get()
        {
            var taskHelper = SystemTaskHelper.Instance;
            var elexonApiKey = TestHelper.ElexonApiKey();
            var elexonDownloader = new ElexonDownloader(taskHelper, elexonApiKey);
            var fuelInstHhCur = new FuelInstHhCur(taskHelper, elexonDownloader);
            var data = await fuelInstHhCur.GetAsync();
            // The following are expected to be correct just about all the time.
            Assert.InRange(data.Nuclear, Power.FromGigawatts(4), Power.FromGigawatts(10));
            Assert.InRange(data.Ccgt, Power.FromGigawatts(4), Power.FromGigawatts(40));
            Assert.InRange(data.Oil, Power.Zero, Power.FromMegawatts(500));
            Assert.InRange(data.Total, Power.FromGigawatts(18), Power.FromGigawatts(60));
            // Check update time is reasonable, from 24 hours ago until now.
            var now = SystemClock.Instance.GetCurrentInstant();
            Assert.InRange(data.Update, now - NodaTime.Duration.FromHours(24), now);
        }
    }
}

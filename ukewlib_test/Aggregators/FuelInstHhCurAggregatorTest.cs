using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Aggregators
{
    public class FuelInstHhCurAggregatorTest
    {
        private static readonly DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

        [Fact]
        public void ByDay()
        {
            var date0 = new LocalDate(2017, 1, 1);
            var instant0 = date0.AtStartOfDayInZone(s_tzLondon).ToInstant();
            var instant1 = instant0 + NodaTime.Duration.FromMinutes(1);
            var instant2 = instant0 + NodaTime.Duration.FromMinutes(2);
            var instant3 = instant0 + NodaTime.Duration.FromMinutes(3);
            var date1 = new LocalDate(2017, 1, 2);
            var instant10 = date1.AtStartOfDayInZone(s_tzLondon).ToInstant();
            var p0 = Power.Zero;
            var p1 = Power.FromWatts(1);
            var p2 = Power.FromWatts(2);
            var data = new [] {
                new FuelInstHhCur.Data(instant0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0),
                new FuelInstHhCur.Data(instant1, p1, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0),
                new FuelInstHhCur.Data(instant2, p0, p2, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0),
                new FuelInstHhCur.Data(instant10, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p1, p2),
            };
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory();
                var writer = new FuelInstHhCur.Writer(th, dir);
                await writer.AppendAsync(data).ConfigureAwait(th);
                var agg = await FuelInstHhCurAggregator.CreateAsync(th, dir).ConfigureAwait(th);
                var list0 = agg.ByDay(date0, date0.PlusDays(1));
                var expected0 = (date0, new FuelInstHhCur.Data(Instant.MinValue, p1, p2, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0));
                Assert.Single(list0, expected0);
                var list1 = agg.ByDay(date1, date1.PlusDays(1));
                var expected1 = (date1, new FuelInstHhCur.Data(Instant.MinValue, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p0, p1, p2));
                Assert.Single(list1, expected1);
                var list2 = agg.ByDay(date0, date0.PlusDays(2));
                Assert.Equal(2, list2.Count);
                Assert.Equal(list2, new [] { expected0, expected1 });
            });
        }
    }
}
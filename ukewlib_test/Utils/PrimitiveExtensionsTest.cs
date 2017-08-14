using NodaTime;
using Xunit;

namespace Ukew.Utils
{
    public class PrimitiveExtensionsTest
    {
        [Fact]
        public void SettlementDate()
        {
            // UK daylight savings started on 27th March 2016
            Assert.Equal(new LocalDate(2016, 3, 26), Instant.FromUtc(2016, 3, 26, 0, 0).SettlementDate());
            Assert.Equal(new LocalDate(2016, 3, 26), Instant.FromUtc(2016, 3, 26, 23, 59).SettlementDate());
            Assert.Equal(new LocalDate(2016, 3, 27), Instant.FromUtc(2016, 3, 27, 0, 0).SettlementDate());
            Assert.Equal(new LocalDate(2016, 3, 27), Instant.FromUtc(2016, 3, 27, 22, 59).SettlementDate());
            Assert.Equal(new LocalDate(2016, 3, 28), Instant.FromUtc(2016, 3, 27, 23, 0).SettlementDate());
            Assert.Equal(new LocalDate(2016, 3, 28), Instant.FromUtc(2016, 3, 28, 22, 59).SettlementDate());
            // UK daylight savings ended on 30th October 2016
            Assert.Equal(new LocalDate(2016, 10, 29), Instant.FromUtc(2016, 10, 29, 22, 59).SettlementDate());
            Assert.Equal(new LocalDate(2016, 10, 30), Instant.FromUtc(2016, 10, 29, 23, 0).SettlementDate());
            Assert.Equal(new LocalDate(2016, 10, 30), Instant.FromUtc(2016, 10, 30, 23, 59).SettlementDate());
            Assert.Equal(new LocalDate(2016, 10, 31), Instant.FromUtc(2016, 10, 31, 0, 0).SettlementDate());
            Assert.Equal(new LocalDate(2016, 10, 31), Instant.FromUtc(2016, 10, 31, 23, 59).SettlementDate());
        }

        [Fact]
        public void SettlementPeriod()
        {
            // UK daylight savings started on 27th March 2016
            Assert.Equal(1, Instant.FromUtc(2016, 3, 26, 0, 0).SettlementPeriod());
            Assert.Equal(48, Instant.FromUtc(2016, 3, 26, 23, 59).SettlementPeriod());
            Assert.Equal(1, Instant.FromUtc(2016, 3, 27, 0, 0).SettlementPeriod());
            Assert.Equal(46, Instant.FromUtc(2016, 3, 27, 22, 59).SettlementPeriod());
            Assert.Equal(1, Instant.FromUtc(2016, 3, 27, 23, 0).SettlementPeriod());
            Assert.Equal(48, Instant.FromUtc(2016, 3, 28, 22, 59).SettlementPeriod());
            // UK daylight savings ended on 30th October 2016
            Assert.Equal(48, Instant.FromUtc(2016, 10, 29, 22, 59).SettlementPeriod());
            Assert.Equal(1, Instant.FromUtc(2016, 10, 29, 23, 0).SettlementPeriod());
            Assert.Equal(3, Instant.FromUtc(2016, 10, 30, 0, 0).SettlementPeriod());
            Assert.Equal(50, Instant.FromUtc(2016, 10, 30, 23, 59).SettlementPeriod());
            Assert.Equal(1, Instant.FromUtc(2016, 10, 31, 0, 0).SettlementPeriod());
            Assert.Equal(48, Instant.FromUtc(2016, 10, 31, 23, 59).SettlementPeriod());
        }

        [Fact]
        public void SettlementPeriodStart()
        {
            // UK daylight savings started on 27th March 2016
            Assert.Equal(Instant.FromUtc(2016, 3, 26, 0, 0), (new LocalDate(2016, 3, 26), 1).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 3, 26, 23, 30), (new LocalDate(2016, 3, 26), 48).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 3, 27, 0, 0), (new LocalDate(2016, 3, 27), 1).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 3, 27, 22, 30), (new LocalDate(2016, 3, 27), 46).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 3, 27, 23, 0), (new LocalDate(2016, 3, 28), 1).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 3, 28, 22, 30), (new LocalDate(2016, 3, 28), 48).SettlementPeriodStart());
            // UK daylight savings ended on 30th October 2016
            Assert.Equal(Instant.FromUtc(2016, 10, 29, 23, 0), (new LocalDate(2016, 10, 30), 1).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 10, 30, 0, 0), (new LocalDate(2016, 10, 30), 3).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 10, 30, 23, 30), (new LocalDate(2016, 10, 30), 50).SettlementPeriodStart());
            Assert.Equal(Instant.FromUtc(2016, 10, 31, 0, 0), (new LocalDate(2016, 10, 31), 1).SettlementPeriodStart());
        }
    }
}

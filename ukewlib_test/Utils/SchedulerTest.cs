using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Utils
{
    public class SchedulerTest
    {
        private Duration S(int seconds) => Duration.FromSeconds(seconds);
        private Instant I(int seconds) => Instant.FromUtc(2000, 1, 1, 0, 0) + S(seconds);

        [Fact]
        public void NextDelay()
        {
            Assert.Equal(S(10), Scheduler.NextDelay(S(10), S(0), I(0)));
            Assert.Equal(S(9), Scheduler.NextDelay(S(10), S(0), I(1)));
            Assert.Equal(S(8), Scheduler.NextDelay(S(10), S(0), I(2)));
            Assert.Equal(S(1), Scheduler.NextDelay(S(10), S(0), I(9)));
            Assert.Equal(S(10), Scheduler.NextDelay(S(10), S(0), I(10)));
            Assert.Equal(S(9), Scheduler.NextDelay(S(10), S(0), I(11)));

            Assert.Equal(S(10), Scheduler.NextDelay(S(10), S(2), I(2)));
            Assert.Equal(S(9), Scheduler.NextDelay(S(10), S(2), I(3)));
            Assert.Equal(S(8), Scheduler.NextDelay(S(10), S(2), I(4)));
            Assert.Equal(S(1), Scheduler.NextDelay(S(10), S(2), I(11)));
            Assert.Equal(S(10), Scheduler.NextDelay(S(10), S(2), I(12)));
            Assert.Equal(S(9), Scheduler.NextDelay(S(10), S(2), I(13)));
        }

        [Fact]
        public void SchdeduledActionShort()
        {
            TimeRunner.Run(async (time, th) =>
            {
                List<Instant> times = new List<Instant>();
                var t0 = time.GetCurrentInstant();
                var scheduler = new Scheduler(time, th);
                while (time.GetCurrentInstant() <= t0 + Duration.FromSeconds(40))
                {
                    await scheduler.ScheduleOne(Duration.FromSeconds(10), Duration.FromSeconds(1)).ConfigureAwait(th);
                    times.Add(time.GetCurrentInstant());
                }
                var expected = new []
                {
                    t0 + S(1),
                    t0 + S(11),
                    t0 + S(21),
                    t0 + S(31),
                    t0 + S(41),
                };
                Assert.Equal(expected, times);
            });
        }

        [Fact]
        public void ScheduledActionOverlong()
        {
            TimeRunner.Run(async (time, th) =>
            {
                List<Instant> times = new List<Instant>();
                var t0 = time.GetCurrentInstant();
                var scheduler = new Scheduler(time, th);
                while (time.GetCurrentInstant() <= t0 + Duration.FromSeconds(40))
                {
                    await scheduler.ScheduleOne(Duration.FromSeconds(10), Duration.FromSeconds(1)).ConfigureAwait(th);
                    times.Add(time.GetCurrentInstant());
                    await th.Delay(Duration.FromSeconds(15)).ConfigureAwait(th);
                }
                var expected = new []
                {
                    t0 + S(1),
                    t0 + S(21),
                    t0 + S(41),
                };
                Assert.Equal(expected, times);
            });
        }
    }
}

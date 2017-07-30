using NodaTime;
using Xunit;
using Ukew.Utils.Tasks;
using System;
using System.Threading.Tasks;
using System.Threading;

namespace Ukew.Testing
{
    public class TestTimeTest
    {
        [Fact]
        public void Delay()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var t0 = time.GetCurrentInstant();
                await th.Delay(Duration.FromSeconds(1)).ConfigureAwait(th);
                Assert.Equal(t0 + Duration.FromSeconds(1), time.GetCurrentInstant());
                await th.Delay(Duration.FromSeconds(10)).ConfigureAwait(th);
                Assert.Equal(t0 + Duration.FromSeconds(11), time.GetCurrentInstant());
                await th.Delay(Duration.FromSeconds(100)).ConfigureAwait(th);
                Assert.Equal(t0 + Duration.FromSeconds(111), time.GetCurrentInstant());
            });
        }

        [Fact]
        public void TooFewThreadsWait()
        {
            Assert.Throws<TestTime.SchedulerException>(() => TimeRunner.Run((time, th) =>
            {
                Task task = th.Run(async () =>
                {
                    await th.Delay(Duration.FromSeconds(10)).ConfigureAwait(th);
                });
                task.Wait(th);
                return Task.CompletedTask;
            }, threadCount: 1));
        }

        [Theory, CombinatorialData]
        public void CancelledDelayWait(
            [CombinatorialValues(2,  3, 7)] int threadCount)
        {
            TimeRunner.Run((time, th) =>
            {
                var t0 = time.GetCurrentInstant();
                var cts = new CancellationTokenSource();
                Task task = th.Run(async () =>
                {
                    await th.Delay(Duration.FromSeconds(10), cts.Token).ConfigureAwait(th);
                });
                cts.Cancel();
                var ae = Assert.Throws<AggregateException>(() => task.Wait(th));
                Assert.IsAssignableFrom<OperationCanceledException>(ae.InnerException);
                Assert.Equal(t0, time.GetCurrentInstant());
                Assert.True(task.IsCanceled);
                return Task.CompletedTask;
            }, threadCount: threadCount);
        }

        [Theory, CombinatorialData]
        public void RunThrowWait(
            [CombinatorialValues(2,  3, 7)] int threadCount)
        {
            TimeRunner.Run(async (time, th) =>
            {
                Task task = th.Run(() => throw new NotImplementedException());
                var ae = Assert.Throws<AggregateException>(() => th.Wait(task));
                Assert.IsType<NotImplementedException>(ae.InnerException);
                await Assert.ThrowsAsync<NotImplementedException>(() => task).ConfigureAwait(th);
                await Assert.ThrowsAsync<NotImplementedException>(async () => await task.ConfigureAwait(th)).ConfigureAwait(th);
                Assert.True(task.IsFaulted);
            }, threadCount: threadCount);
        }
    }
}

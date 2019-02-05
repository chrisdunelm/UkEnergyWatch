using System;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using UnitsNet;
using Xunit;

namespace Ukew.Elexon
{
    public class FuelInstHhCurTest
    {
        [Fact]
        public void Download()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var fuelInstHhCur = new FuelInstHhCur(th, new FakeElexonDownloader());
                var data = await fuelInstHhCur.GetAsync().ConfigureAwait(th);
                Assert.Equal(Instant.FromUtc(2017, 5, 11, 17, 55, 0), data.Update);
                Assert.Equal(Power.FromMegawatts(18384), data.Ccgt);
                Assert.Equal(Power.FromMegawatts(0), data.Ocgt);
                Assert.Equal(Power.FromMegawatts(0), data.Oil);
                Assert.Equal(Power.FromMegawatts(645), data.Coal);
                Assert.Equal(Power.FromMegawatts(6552), data.Nuclear);
                Assert.Equal(Power.FromMegawatts(2714), data.Wind);
                Assert.Equal(Power.FromMegawatts(1193), data.Ps);
                Assert.Equal(Power.FromMegawatts(254), data.Npshyd);
                Assert.Equal(Power.FromMegawatts(1443), data.Other);
                Assert.Equal(Power.FromMegawatts(1997), data.IntFr);
                Assert.Equal(Power.FromMegawatts(78), data.IntIrl);
                Assert.Equal(Power.FromMegawatts(989), data.IntNed);
                Assert.Equal(Power.FromMegawatts(0), data.IntEw);
            });
        }

        [Fact]
        public void StoreAndLoad()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory(th);
                var fuelInstHhCur = new FuelInstHhCur(th, new FakeElexonDownloader());
                var data = await fuelInstHhCur.GetAsync().ConfigureAwait(th);
                var writer = new FuelInstHhCur.Writer(th, dir);
                var reader = new FuelInstHhCur.Reader(th, dir);
                Assert.Equal(0, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(0, await (await reader.ReadAsync().ConfigureAwait(th)).Count().ConfigureAwait(th));
                await writer.AppendAsync(data).ConfigureAwait(th);
                Assert.Equal(1, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(1, await (await reader.ReadAsync().ConfigureAwait(th)).Count().ConfigureAwait(th));
                var data1 = await (await reader.ReadAsync().ConfigureAwait(th)).Last().ConfigureAwait(th);
                Assert.Equal(data, data1);
                var data2 = await (await reader.ReadAsync(0, 1).ConfigureAwait(th)).Single().ConfigureAwait(th);
                Assert.Equal(data, data2);
            });
        }
    }
}

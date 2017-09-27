using System.Linq;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.MemDb
{
    public class DbTest
    {
        [Fact]
        public void VerySimple() => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int> { 0, 1, 2, 3 };
            using (var db = new Db<int>(th, reader))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 2, 3 }, db.Where(x => x >= 2));
            }
        });

        [Fact]
        public void Large() => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>();
            reader.AddRange(Enumerable.Range(0, 100_000));
            using (var db = new Db<int>(th, reader, blockSize: 99))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 0, 1 }, db.Where(x => x < 2));
                Assert.Equal(new [] { 99_998, 99_999 }, db.Where(x => x >= 99_998));
                Assert.Equal(50_000, db.Where(x => (x & 1) == 0).Length);
                Assert.Equal(new [] { 0.0, 1.0 }, db.WhereSelect(x => x < 2 ? (double?)x : null));
            }
        });
    }
}

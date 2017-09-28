using System.Linq;
using NodaTime;
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
        public void FewInt() => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int> { 0, 1, 2, 3 };
            using (var db = new Db<int>(th, reader))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 2, 3 }, db.Where(x => x >= 2));
            }
        });

        [Theory, CombinatorialData]
        public void ManyInt(
            [CombinatorialValues(0, 1, 100, 1000)] int blockSize
        ) => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>();
            reader.AddRange(Enumerable.Range(0, 100_000));
            using (var db = new Db<int>(th, reader, blockSize: blockSize))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 0, 1 }, db.Where(x => x < 2));
                Assert.Equal(new [] { 99_998, 99_999 }, db.Where(x => x >= 99_998));
                Assert.Equal(50_000, db.Where(x => (x & 1) == 0).Length);
                Assert.Equal(new [] { 0.0, 1.0 }, db.WhereSelect(x => x < 2 ? (double?)x : null));
            }
        });

        private struct Data
        {
            public int a;
            public byte b;
            public long c;
            public short d;
        }

        [Theory, CombinatorialData]
        public void ManyStruct(
            [CombinatorialValues(0, 1, 100, 1000)] int blockSize,
            [CombinatorialValues(2, 3_004, 100_001)] int dataSize
        ) => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<Data>();
            reader.AddRange(Enumerable.Range(0, dataSize).Select(i => new Data { a = i, b = (byte)i, c = i, d = (short)i }));
            using (var db = new Db<Data>(th, reader, blockSize: blockSize))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 0L, 1L }, db.Where(x => x.a < 2).Select(x => x.c));
                Assert.Equal(new long [] { dataSize - 2, dataSize - 1 }, db.Where(x => x.a >= dataSize - 2).Select(x => x.c));
                Assert.Equal((dataSize + 1) / 2, db.Where(x => (x.b & 1) == 0).Length);
                Assert.Equal(new [] { 0.0, 1.0 }, db.WhereSelect(x => x.a < 2 ? (double?)x.d : null));
            }
        });

        [Theory, CombinatorialData]
        public void PollNewData(
            [CombinatorialValues(0, 1, 100, 1000)] int blockSize,
            [CombinatorialValues(0, 1, 99, 101)] int loadCycles
        ) => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>();
            using (var db = new Db<int>(th, reader, blockSize: blockSize, pollInterval: Duration.FromMinutes(5), maxJitter: Duration.Zero))
            {
                await th.Delay(Duration.FromMinutes(1)).ConfigureAwait(th);
                for (int i = 0; i < loadCycles; i += 1)
                {
                    var count0 = reader.Count;
                    reader.AddRange(Enumerable.Repeat(i, i));
                    await th.Delay(Duration.FromMinutes(3)).ConfigureAwait(th);
                    Assert.Equal(reader.Take(count0), db.Where(_ => true));
                    await th.Delay(Duration.FromMinutes(2)).ConfigureAwait(th);
                    Assert.Equal(reader, db.Where(_ => true));
                }
            }
        });
    }
}

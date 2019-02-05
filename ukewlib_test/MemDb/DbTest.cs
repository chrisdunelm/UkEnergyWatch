using System;
using System.Collections.Immutable;
using System.Linq;
using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils;
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
            using (var db = new Db<int>(th, reader, disableWatch: true))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 2, 3 }, db.Where(x => x >= 2).ToImmutableArray());
            }
        });

        [Theory, CombinatorialData]
        public void ManyInt(
            [CombinatorialValues(0, 1, 100, 1000)] int blockSize
        ) => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>();
            reader.AddRange(Enumerable.Range(0, 100_000));
            using (var db = new Db<int>(th, reader, requestedBlockSize: blockSize))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 0, 1 }, db.Where(x => x < 2).ToImmutableArray());
                Assert.Equal(new [] { 99_998, 99_999 }, db.Where(x => x >= 99_998).ToImmutableArray());
                Assert.Equal(50_000, db.Where(x => (x & 1) == 0).ToImmutableArray().Length);
                //Assert.Equal(new [] { 0.0, 1.0 }, db.WhereSelect(x => x < 2 ? (double?)x : null).ToImmutableArray());
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
            using (var db = new Db<Data>(th, reader, requestedBlockSize: blockSize))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(new [] { 0L, 1L }, db.Where(x => x.a < 2).ToImmutableArray().Select(x => x.c));
                Assert.Equal(new long [] { dataSize - 2, dataSize - 1 }, db.Where(x => x.a >= dataSize - 2).ToImmutableArray().Select(x => x.c));
                Assert.Equal((dataSize + 1) / 2, db.Where(x => (x.b & 1) == 0).ToImmutableArray().Length);
                Assert.Equal(new [] { 0.0, 1.0 }, db.WhereSelect(x => x.a < 2 ? (double?)x.d : null).ToImmutableArray());
            }
        });

        [Fact]
        public void PollNewData_Simple() => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>();
            using (var db = new Db<int>(th, reader, pollInterval: Duration.FromMinutes(5), maxJitter: Duration.Zero, disableWatch: true))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Empty(db.AsEnumerable());
                reader.Add(1);
                await th.Delay(Duration.FromMinutes(3)).ConfigureAwait(th);
                Assert.Empty(db.AsEnumerable());
                await th.Delay(Duration.FromMinutes(3)).ConfigureAwait(th);
                Assert.Single(db.AsEnumerable(), 1);
            }
        });

        [Theory, CombinatorialData]
        public void PollNewData_Complex(
            [CombinatorialValues(0, 1, 100, 1000)] int blockSize,
            [CombinatorialValues(0, 1, 99, 101)] int loadCycles
        ) => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>();
            using (var db = new Db<int>(th, reader, requestedBlockSize: blockSize, pollInterval: Duration.FromMinutes(5), maxJitter: Duration.Zero))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                await th.Delay(Duration.FromMinutes(1)).ConfigureAwait(th);
                var expectedCount = 0;
                for (int i = 0; i < loadCycles; i += 1)
                {
                    var count0 = reader.Count;
                    reader.AddRange(Enumerable.Repeat(i, i));
                    await th.Delay(Duration.FromMinutes(3)).ConfigureAwait(th);
                    Assert.Equal(expectedCount, db.AsEnumerable().Count());
                    Assert.Equal(expectedCount + i, reader.Count);
                    Assert.Equal(reader.Take(count0), db.Where(_ => true).ToImmutableArray());
                    await th.Delay(Duration.FromMinutes(2)).ConfigureAwait(th);
                    expectedCount += i;
                    Assert.Equal(expectedCount, db.AsEnumerable().Count());
                    Assert.Equal(reader, db.ToImmutableArray());
                }
            }
        });

        private struct IntData : IStorable<IntData, IntData>, IStorableFactory<IntData>, IEquatable<IntData>
        {
            public int Value { get; private set; }

            public ImmutableArray<byte> Store(IntData data) => throw new NotImplementedException();
            public IntData Load(int version, ReadOnlySpan<byte> data) => new IntData { Value = 42 };
            public int CurrentVersion => 1;
            public bool Equals(IntData other) => Value == other.Value;
        }

        private class IntReader : DataStoreReader<IntData, IntData>
        {
            public IntReader(ITaskHelper taskHelper, IDirectory dir) : base(taskHelper, dir, "int") { }
        }

        [Fact]
        public void Watcher() => TimeRunner.Run(async (time, th) =>
        {
            var filename = "int.seqid.00000001.version.1.elementsize.8.datastore";
            var e = Bits.Empty;
            var idBits = e.Concat(ImmutableArray.Create(DataStore.ID_BYTE_1, DataStore.ID_BYTE_2));
            var data = e.Concat(idBits).Concat(e.AddInt(314).AddFletcher16);

            var dir = new FakeDirectory(th);
            var reader = new IntReader(th, dir);
            using (var db = new Db<IntData>(th, reader))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Empty(db.AsEnumerable());
                await dir.AppendAsync(filename, data).ConfigureAwait(th);
                await th.Delay(Duration.FromSeconds(1)).ConfigureAwait(th);
                Assert.Single(db.AsEnumerable());
            }
        });
    }
}

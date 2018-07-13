using System;
using System.Collections.Immutable;
using System.Linq;
using System.Threading.Tasks;
using Ukew.Testing;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Storage
{
    public class DataStoreReaderTest
    {
        [Fact]
        public void ReadEmpty()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory();
                var reader = new Data.Reader(th, dir);
                var data = await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th);
                Assert.Empty(data);
            });
        }

        [Fact]
        public void AddEntries()
        {
            TimeRunner.Run(async (time, th) =>
            {
                const string filename = "data.seqid.00000001.version.1.elementsize.8.datastore";
                var e = Bits.Empty;
                var idBits = e.Concat(ImmutableArray.Create(DataStore.ID_BYTE_1, DataStore.ID_BYTE_2));
                var data = e.Concat(idBits).Concat(e.AddInt(100).AddFletcher16);
                var dir = new FakeDirectory();
                var reader = new Data.Reader(th, dir);
                Assert.Empty(await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                await dir.AppendAsync(filename, data);
                Assert.Equal(1, await reader.CountAsync().ConfigureAwait(th));
                Assert.Single(await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                await dir.AppendAsync(filename, data);
                Assert.Equal(2, await reader.CountAsync().ConfigureAwait(th));
                Assert.Equal(2, (await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th)).Count());
            });
        }

        private (IDirectory, Data[]) BuildData()
        {
            var version1 = "data.seqid.00000001.version.1.elementsize.8.datastore";
            var version2 = "data.seqid.00000002.version.2.elementsize.d.datastore";
            var version3 = "data.seqid.00000003.version.3.elementsize.12.datastore";
            var e = Bits.Empty;
            var idBits = e.Concat(ImmutableArray.Create(DataStore.ID_BYTE_1, DataStore.ID_BYTE_2));
            var dir = new FakeDirectory(
                (version1, e.Concat(idBits).Concat(e.AddInt(100).AddFletcher16).Concat(idBits).Concat(e.AddInt(101).AddFletcher16)),
                (version2, e.Concat(idBits).Concat(e.AddInt(200).AddIntN(201).AddFletcher16).Concat(idBits).Concat(e.AddInt(300).AddIntN(301).AddFletcher16)),
                (version3, e.Concat(idBits).Concat(e.AddInt(400).AddIntN(401).AddIntN(402).AddFletcher16))
            );
            var expected = new[] {
                new Data(100, null, null),
                new Data(101, null, null),
                new Data(200, 201, null),
                new Data(300, 301, null),
                new Data(400, 401, 402),
            };
            return (dir, expected);
        }

        [Fact]
        public void ReadThreeVersions()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var (dir, expected) = BuildData();
                var reader = new Data.Reader(th, dir);
                var data = await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th);
                Assert.Equal(expected, data);
            });
        }

        [Fact]
        public void ReadPartial()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var (dir, expected) = BuildData();
                var reader = new Data.Reader(th, dir);
                async Task<Data[]> Read(int from, int to) =>
                    await (await reader.ReadAsync(from, to).ConfigureAwait(th)).ToArray().ConfigureAwait(th);
                for (int start = 0; start < 5; start++)
                {
                    for (int end = start; end < 5; end++)
                    {
                        Assert.Equal(expected.Skip(start).Take(end - start).ToArray(), await Read(start, end).ConfigureAwait(th));
                    }
                }
            });
        }

        [Fact]
        public void ReadCount()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var (dir, _) = BuildData();
                var reader = new Data.Reader(th, dir);
                Assert.Equal(5, await reader.CountAsync().ConfigureAwait(th));
            });
        }
    }
}

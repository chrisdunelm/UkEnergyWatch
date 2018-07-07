using System.Collections.Immutable;
using System.Linq;
using System.Threading.Tasks;
using Ukew.Testing;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Storage
{
    public class DataStorageWriterTest
    {
        [Fact]
        public void WriterReadSingleVersion()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory();
                var writer = new Data.Writer(th, dir);
                var reader = new Data.Reader(th, dir);
                Assert.Empty(await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                await writer.AppendAsync(new Data(1, 2, 3)).ConfigureAwait(th);
                Assert.Equal(new [] { new Data(1, 2, 3) }, await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                await writer.AppendAsync(new Data(4, 5, 6)).ConfigureAwait(th);
                Assert.Equal(new [] { new Data(1, 2, 3), new Data(4, 5, 6) }, await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                Assert.Single(await dir.ListFilesAsync().ConfigureAwait(th));
            });
        }
        
        [Fact]
        public void WriterWithPreviousVersion()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var idBits = ImmutableArray.Create(DataStore.ID_BYTE_1, DataStore.ID_BYTE_2);
                var dir = new FakeDirectory(
                    ("data.seqid.00000001.version.1.elementsize.8.datastore", Bits.Empty.Concat(idBits).Concat(Bits.Empty.AddInt(1).AddFletcher16))
                );
                var writer = new Data.Writer(th, dir);
                var reader = new Data.Reader(th, dir);
                Assert.Equal(new [] { new Data(1, null, null) }, await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
                await writer.AppendAsync(new Data(4, 5, 6)).ConfigureAwait(th);
                Assert.Equal(new [] { new Data(1, null, null), new Data(4, 5, 6) }, await (await reader.ReadAsync().ConfigureAwait(th)).ToArray().ConfigureAwait(th));
            });
        }
    }
}
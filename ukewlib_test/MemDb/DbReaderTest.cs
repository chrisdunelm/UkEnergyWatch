using System.Linq;
using Xunit;

namespace Ukew.MemDb
{
    public class DbReaderTest
    {
        [Fact]
        public void Where()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(new [] { 1, 2 }, dbSmall.Where(x => x <= 2).ToImmutableArray());
            var dbLarge = new FakeDbReader<int>(Enumerable.Range(1, 10_000));
            Assert.Equal(new [] { 1, 2 }, dbLarge.Where(x => x <= 2).ToImmutableArray());
        }

        [Fact]
        public void WhereSelect()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(new byte[] { 1, 2 }, dbSmall.WhereSelect(x => x <= 2 ? (byte)x : default(byte?)).ToImmutableArray());
            var dbLarge = new FakeDbReader<int>(Enumerable.Range(1, 10_000));
            Assert.Equal(new byte[] { 1, 2 }, dbLarge.WhereSelect(x => x <= 2 ? (byte)x : default(byte?)).ToImmutableArray());
        }

        [Fact]
        public void GroupBy()
        {
            var db = new FakeDbReader<int>(Enumerable.Range(1, 10_000));
            var grouped = db.GroupBy(x => x <= 2);
            Assert.Equal(new [] { 1, 2 }, grouped[true].ToImmutableArray());
            Assert.Equal(9_998, grouped[false].Count);
            var agged = db.GroupBy(x => x <= 2, x => x.ToImmutableArray().Sum());
            Assert.Equal(3, agged[true]);
            Assert.Equal(10_001 * 5_000 - 3, agged[false]);
        }
    }
}

using System.Linq;
using Xunit;

namespace Ukew.MemDb
{
    public class DbReaderTest
    {
        [Fact]
        public void ToImmutableArray()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(new [] { 1, 2, 3, 4 }, dbSmall.ToImmutableArray());
        }

        [Fact]
        public void FirstOrDefault()
        {
            var dbEmpty = new FakeDbReader<int>();
            Assert.Null(dbEmpty.FirstOrDefault());
            Assert.Null(dbEmpty.ReverseView.FirstOrDefault());
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(1, dbSmall.FirstOrDefault());
            Assert.Equal(4, dbSmall.ReverseView.FirstOrDefault());
            Assert.Null(dbSmall.Where(x => false).FirstOrDefault());
            Assert.Null(dbSmall.ReverseView.Where(x => false).FirstOrDefault());
            var dbLarge = new FakeDbReader<int>(Enumerable.Range(1, 10_000));
            Assert.Equal(1, dbLarge.FirstOrDefault());
            Assert.Equal(10_000, dbLarge.ReverseView.FirstOrDefault());
            Assert.Null(dbLarge.Where(x => false).FirstOrDefault());
            Assert.Null(dbLarge.ReverseView.Where(x => false).FirstOrDefault());
        }

        [Fact]
        public void Select()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(new byte[] { 1, 2, 3, 4 }, dbSmall.Select(x => (byte)x).ToImmutableArray());
            var largeRange = Enumerable.Range(1, 10_000);
            var dbLarge = new FakeDbReader<int>(largeRange);
            Assert.Equal(largeRange.Select(x => (byte)x), dbLarge.Select(x => (byte)x).ToImmutableArray());
        }

        [Fact]
        public void Where()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(new [] { 1, 2 }, dbSmall.Where(x => x <= 2).ToImmutableArray());
            Assert.Equal(new int[0], dbSmall.Where(x => x > 10).ToImmutableArray());
            Assert.Equal(new int[0], dbSmall.Where(x => x <= 2).Where(x => x > 2).ToImmutableArray());
            var dbLarge = new FakeDbReader<int>(Enumerable.Range(1, 10_000));
            Assert.Equal(new [] { 1, 2 }, dbLarge.Where(x => x <= 2).ToImmutableArray());
            Assert.Equal(dbLarge.Count - 2, dbLarge.Where(x => x > 2).ToImmutableArray().Length);
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
        public void Reverse()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Equal(new [] { 4, 3, 2, 1 }, dbSmall.ReverseView.ToImmutableArray());
            var largeRange = Enumerable.Range(1, 10_000);
            var dbLarge = new FakeDbReader<int>(largeRange);
            Assert.Equal(largeRange.Reverse(), dbLarge.ReverseView.ToImmutableArray());
        }

        [Fact]
        public void Take()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            Assert.Empty(dbSmall.Take(0).ToImmutableArray());
            Assert.Equal(new [] { 1, 2, 3 }, dbSmall.Take(3).ToImmutableArray());
            Assert.Equal(new [] { 1, 2, 3, 4 }, dbSmall.Take(4).ToImmutableArray());
            Assert.Equal(new [] { 1, 2, 3, 4 }, dbSmall.Take(5).ToImmutableArray());
            var largeRange = Enumerable.Range(1, 10_000);
            var dbLarge = new FakeDbReader<int>(largeRange);
            Assert.Empty(dbLarge.Take(0).ToImmutableArray());
            Assert.Equal(largeRange.Take(9), dbLarge.Take(9).ToImmutableArray());
            Assert.Equal(largeRange.Take(99), dbLarge.Take(99).ToImmutableArray());
            Assert.Equal(largeRange.Take(999), dbLarge.Take(999).ToImmutableArray());
            Assert.Equal(largeRange.Take(9999), dbLarge.Take(9999).ToImmutableArray());
            Assert.Equal(largeRange.Take(99999), dbLarge.Take(99999).ToImmutableArray());
        }

        [Fact]
        public void Multi()
        {
            var dbSmall = new FakeDbReader<int>(1, 2, 3, 4);
            var take = dbSmall.Take(2);
            Assert.Equal(new[]{1, 2}, take.ToImmutableArray());
            Assert.Equal(new[]{1, 2}, take.ToImmutableArray());
        }
    }
}

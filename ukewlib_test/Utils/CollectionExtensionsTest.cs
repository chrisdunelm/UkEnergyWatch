using Xunit;

namespace Ukew.Utils
{
    public class CollectionExtensionTest
    {
        [Fact]
        public void BinarySearch()
        {
           var a = new [] { "a", "bb", "ccc" };
           //Assert.Equal(-1, a.BinarySearch(0, (s, l) => l - s.Length));
           Assert.Equal(0, a.BinarySearch(1, (s, l) => l - s.Length));
           Assert.Equal(1, a.BinarySearch(2, (s, l) => l - s.Length));
           Assert.Equal(2, a.BinarySearch(3, (s, l) => l - s.Length));
           //Assert.Equal(3, a.BinarySearch(4, (s, l) => l - s.Length));
        }
    }
}

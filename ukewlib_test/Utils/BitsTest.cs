using Xunit;

namespace Ukew.Utils
{
    public class BitsTest
    {
        [Theory, CombinatorialData]
        public void Uint([CombinatorialValues(0, 1, uint.MaxValue, uint.MaxValue - 1)] uint i)
        {
            var bits = Bits.Empty.AddUInt(i);
            Assert.Equal(i, Bits.GetUInt(bits, 0));
        }

        [Theory, CombinatorialData]
        public void Int([CombinatorialValues(0, 1, -1, int.MinValue, int.MinValue + 1, int.MaxValue, int.MaxValue - 1)] int i)
        {
            var bits = Bits.Empty.AddInt(i);
            Assert.Equal(i, Bits.GetInt(bits, 0));
        }

        [Theory, CombinatorialData]
        public void Ushort([CombinatorialValues(0, 1, ushort.MaxValue, ushort.MaxValue - 1)] ushort i)
        {
            var bits = Bits.Empty.AddUShort(i);
            Assert.Equal(i, Bits.GetUShort(bits, 0));
        }

        [Theory, CombinatorialData]
        public void Short([CombinatorialValues(0, 1, -1, short.MinValue, short.MinValue + 1, short.MaxValue, short.MaxValue - 1)] short i)
        {
            var bits = Bits.Empty.AddShort(i);
            Assert.Equal(i, Bits.GetShort(bits, 0));
        }
    }
}

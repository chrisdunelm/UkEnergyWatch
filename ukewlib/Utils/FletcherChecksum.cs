using System.Collections.Immutable;

namespace Ukew.Utils
{
    public static class FletcherChecksum
    {
        public static ushort Calc16(ImmutableArray<byte> bytes)
        {
            // https://en.wikipedia.org/wiki/Fletcher%27s_checksum#Optimizations
            ushort sum1 = 0xff;
            ushort sum2 = 0xff;
            int count = bytes.Length;
            int index = 0;
            while (count > 0)
            {
                int tLen = count >= 20 ? 20 : count;
                count -= tLen;
                do
                {
                    sum1 += bytes[index];
                    index += 1;
                    sum2 += sum1;
                    tLen -= 1;
                }
                while (tLen > 0);
                sum1 = (ushort)((sum1 & 0xff) + (sum1 >> 8));
                sum2 = (ushort)((sum2 & 0xff) + (sum2 >> 8));
            }
            sum1 = (ushort)((sum1 & 0xff) + (sum1 >> 8));
            sum2 = (ushort)((sum2 & 0xff) + (sum2 >> 8));
            return (ushort)((sum2 << 8) | sum1);
        }

        public static ImmutableArray<byte> Calc16Bytes(ImmutableArray<byte> bytes)
        {
            ushort f = Calc16(bytes);
            return (new [] { (byte)(f & 0xff), (byte)(f >> 8) }).ToImmutableArray();
        }
    }
}

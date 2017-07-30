using System;
using System.Collections.Generic;

namespace Ukew.Utils
{
    public static class CollectionExtentions
    {
        public static int BinarySearch<A, B>(this IReadOnlyList<A> list, B value, Func<A, B, int> compare)
        {
            if (list.Count == 0)
            {
                return -1;
            }
            int a = 0;
            int b = list.Count;
            int prevM = -1;
            while (true)
            {
                int m = a + (b - a) / 2;
                if (prevM == m)
                {
                    throw new Exception("Failed to find value");
                }
                prevM = m;
                var c = compare(list[m], value);
                // c > 0 if B > A
                if (c == 0)
                {
                    return m;
                }
                if (a >= b)
                {
                    throw new Exception("Failed to find value");
                }
                if (c > 0)
                {
                    a = m + 1;
                }
                else
                {
                    b = m;
                }
            }
        }
    }
}

using System.Collections.Generic;
using System.Collections.Immutable;

namespace Ukew.MemDb
{
    public interface IDbReader<T> where T : struct
    {
        IReadOnlyList<(int blockSize, T[] block)> GetBlocks();
    }
}

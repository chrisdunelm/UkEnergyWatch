using System.Collections.Generic;

namespace Ukew.MemDb
{
    public interface IChunkedEnumerable<T>
    {
        IEnumerator<Chunk<T>> GetEnumerator();
    }
}

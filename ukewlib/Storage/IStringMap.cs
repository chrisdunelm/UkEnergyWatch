using System.Collections.Generic;

namespace Ukew.Storage
{
    public interface IStringMap<T>
    {
        T CloneWithStrings(IReadOnlyList<long> indexes);
    }
}

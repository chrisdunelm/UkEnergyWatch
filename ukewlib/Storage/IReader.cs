using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Ukew.Storage
{
    public interface IReader<T>
    {
        Task<long> CountAsync(CancellationToken ct = default);
        Task<IAsyncEnumerable<T>> ReadAsync(int fromIndex = 0, int toIndex = int.MaxValue, CancellationToken ct = default);
        Task AwaitChange(CancellationToken ct = default);
    }
}

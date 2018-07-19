using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace Ukew.Storage
{
    public class FakeReader<T> : List<T>, IReader<T>
    {
        public Task<long> CountAsync(CancellationToken ct = default) => Task.FromResult((long)Count);

        public Task<IAsyncEnumerable<T>> ReadAsync(int fromIndex = 0, int toIndex = int.MaxValue, CancellationToken ct = default) =>
            Task.FromResult(this.Skip(fromIndex).Take(toIndex - fromIndex).ToList().ToAsyncEnumerable());

        public Task AwaitChange(CancellationToken ct)
        {
            var tcs = new TaskCompletionSource<int>();
            ct.Register(() => tcs.SetCanceled());
            return tcs.Task;
        }
    }
}

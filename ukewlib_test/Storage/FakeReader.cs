using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class FakeReader<T> : List<T>, IReader<T>
    {
        public FakeReader(ITaskHelper th) => _th = th;

        private ITaskHelper _th;

        public Task<long> CountAsync(CancellationToken ct = default) => Task.FromResult((long)Count);

        public Task<IAsyncEnumerable<T>> ReadAsync(int fromIndex = 0, int toIndex = int.MaxValue, CancellationToken ct = default) =>
            Task.FromResult(this.Skip(fromIndex).Take(toIndex - fromIndex).ToList().ToAsyncEnumerable());

        public Task AwaitChange(CancellationToken ct = default)
        {
            var tcs = new TaskCompletionSource<int>();
            ct.Register(() => tcs.SetCanceled());
            return tcs.Task;
        }

        public Task AwaitChange(Duration timeout, CancellationToken ct = default) => _th.Delay(timeout, ct);
    }
}

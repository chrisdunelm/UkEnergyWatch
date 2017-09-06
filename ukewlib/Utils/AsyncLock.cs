using System;
using System.Threading;
using System.Threading.Tasks;

namespace Ukew.Utils
{
    public class AsyncLock
    {
        private class Locked : IDisposable
        {
            public Locked(SemaphoreSlim sem) => _sem = sem;

            private readonly SemaphoreSlim _sem;

            public void Dispose() => _sem.Release();
        }

        private readonly SemaphoreSlim _sem = new SemaphoreSlim(1, 1);

        public async Task<IDisposable> LockAsync(CancellationToken ct)
        {
            await _sem.WaitAsync(ct);
            return new Locked(_sem);
        }
    }
}

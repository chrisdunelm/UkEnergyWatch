using System;

namespace Ukew.Utils
{
    public class DisposeFn : IDisposable
    {
        public DisposeFn(Action fn) => _fn = fn;

        private readonly Action _fn;

        public void Dispose() => _fn();
    }
}

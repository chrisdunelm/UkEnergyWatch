using System;
using System.Linq;
using System.Threading.Tasks;

namespace Ukew.Testing
{
    public static class Extensions
    {
        public static void Locked<TLock>(this TLock o, Action fn) where TLock : class
        {
            lock (o)
            {
                fn();
            }
        }

        public static T Locked<TLock, T>(this TLock o, Func<T> fn) where TLock : class
        {
            lock (o)
            {
                return fn();
            }
        }
    }
}

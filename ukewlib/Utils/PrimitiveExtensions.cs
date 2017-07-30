using System;

namespace Ukew.Utils
{
    public static class PrimitiveExtensions
    {
        public static int InRange(this int i, int min, int max) => i < min ? min : i > max ? max : i;

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

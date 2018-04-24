using System;
using System.Linq;

namespace Ukew.Utils
{
    public static class ExceptionExtensions
    {
        public static bool Is<T>(this Exception e) where T : Exception =>
            e is T || (e is AggregateException ae && ae.Flatten().InnerExceptions.Any(x => x is T));
    }
}

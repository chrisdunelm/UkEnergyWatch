using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Ukew.Utils.Tasks;

namespace Ukew.Utils
{
    public static class LinqExtensions
    {
        public static async Task<IEnumerable<TResult>> SelectAsync<T, TResult>(
            this IEnumerable<T> en, ITaskHelper taskHelper, Func<T, Task<TResult>> fn)
        {
            List<TResult> result = new List<TResult>();
            foreach (var item in en)
            {
                result.Add(await fn(item).ConfigureAwait(taskHelper));
            }
            return result;
        }
    }
}

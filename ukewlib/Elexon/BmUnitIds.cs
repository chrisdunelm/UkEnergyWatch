using System.Collections.Generic;

namespace Ukew.Elexon
{
    // TODO: This will almost certainly change to become a datastore lookup, not hard-coded.
    public static class BmUnitIds
    {
        public static uint Hash(string s)
        {
            // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
            ulong hash = 0xcbf29ce484222325UL;
            for (int i = 0; i < s.Length; i += 1)
            {
                hash *= 1099511628211UL;
                hash ^= s[i];
            }
            return (uint)(hash ^ (hash >> 32));
        }

        public static string Lookup(uint hash) =>
            s_lookup.TryGetValue(hash, out string s) ? s : hash.ToString();

        private static Dictionary<uint, string> s_lookup = new Dictionary<uint, string>
        {
        };
    }
}

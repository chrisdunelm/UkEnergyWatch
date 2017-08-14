using System.Collections.Generic;
using System.Linq;
using Ukew.Elexon;

namespace Ukew.Utils
{
    public class B1610Seen
    {
        public B1610Seen(B1610.Reader reader)
        {
            // TODO: Don't do this. Do it offline.
            var data = reader.ReadAsync().Result.ToEnumerable();
            AllResourceNames = new HashSet<string>(data.Select(x => x.ResourceName));
        }

        public HashSet<string> AllResourceNames { get; }
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xunit;

namespace Ukew.Elexon
{
    public class EicIdsTest
    {
        [Fact]
        public void CheckHashDups()
        {
            var seenHashes = new HashSet<uint>();
            foreach (var hash in EicIds.GenerationUnitsByResourceNameHash.Keys)
            {
                Assert.True(seenHashes.Add(hash), $"Duplicate hash: 0x{hash.ToString("x8")}");
            }
        }
    }
}

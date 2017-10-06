using System.Collections.Generic;

namespace Ukew.MemDb
{
    public class FakeDbReader<T> : DbReader<T> where T : struct
    {
        public FakeDbReader(params T[] data) : this((IEnumerable<T>)data) { }

        public FakeDbReader(IEnumerable<T> data) : base(100)
        {
            foreach (var item in data)
            {
                Add(item);
            }
        }
    }
}

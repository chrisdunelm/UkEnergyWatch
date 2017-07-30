using System.Threading;
using System.Threading.Tasks;
using NodaTime;

namespace Ukew.Utils
{
    public class SystemTime : ITime
    {
        public static SystemTime Instance { get; } = new SystemTime();

        private SystemTime() { }

        public Instant GetCurrentInstant() => SystemClock.Instance.GetCurrentInstant();
    }
}

using System.Threading;
using System.Threading.Tasks;
using NodaTime;

namespace Ukew.Utils
{
    public interface ITime
    {
        Instant GetCurrentInstant();
    }
}

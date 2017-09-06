using System.Threading;
using System.Threading.Tasks;

namespace Ukew.Logging
{
    public interface ITextAppender
    {
        Task AppendLineAsync(string line, CancellationToken ct = default(CancellationToken));
    }
}

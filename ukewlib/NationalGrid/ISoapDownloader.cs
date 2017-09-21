using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;

namespace Ukew.NationalGrid
{
    public interface ISoapDownloader
    {
        Task<XElement> GetSoapAsync(string uri, string soapAction, string soapRequestBody, CancellationToken ct = default(CancellationToken));
    }
}
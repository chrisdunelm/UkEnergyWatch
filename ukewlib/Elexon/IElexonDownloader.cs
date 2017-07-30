using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;

namespace Ukew.Elexon
{
    public interface IElexonDownloader
    {
        Task<XDocument> GetXmlAsync(string reportName, IDictionary<string, string> getParams, CancellationToken ct = default(CancellationToken));
    }
}

using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;

namespace Ukew.NationalGrid
{
    public class FakeSoapDownloader : ISoapDownloader
    {
        public FakeSoapDownloader(Dictionary<(string, string, string), XElement> entries)
        {
            _entries = entries;
        }

        private readonly Dictionary<(string, string, string), XElement> _entries;

        public Task<XElement> GetSoapAsync(string uri, string soapAction, string soapRequestBody, CancellationToken ct= default(CancellationToken))
        {
            return Task.FromResult(_entries[(uri, soapAction, soapRequestBody)]);
        }
    }
}
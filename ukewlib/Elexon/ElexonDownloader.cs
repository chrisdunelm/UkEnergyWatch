using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;
using Ukew.Utils.Tasks;

namespace Ukew.Elexon
{
    public class ElexonDownloader : IElexonDownloader
    {
        public ElexonDownloader(ITaskHelper taskHelper, string apiKey)
        {
            _taskHelper = taskHelper;
            _apiKey = apiKey;
        }

        private ITaskHelper _taskHelper;
        private string _apiKey;

        public async Task<XDocument> GetXmlAsync(string reportName, IDictionary<string, string> getParams, CancellationToken ct = default(CancellationToken))
        {
            var paramString = getParams.Aggregate("", (acc, kv) => $"{acc}&{kv.Key}={kv.Value}");
            var uri = $"https://api.bmreports.com/BMRS/{reportName}/v1?APIKey={_apiKey}{paramString}&ServiceType=xml";
            var httpClient = new HttpClient();
            var response = await httpClient.GetByteArrayAsync(uri).ConfigureAwait(_taskHelper);
            var responseStream = new MemoryStream(response);
            return XDocument.Load(responseStream);
        }
    }
}

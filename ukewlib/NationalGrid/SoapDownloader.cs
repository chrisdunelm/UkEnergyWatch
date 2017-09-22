using System;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;
using Ukew.Utils.Tasks;

namespace Ukew.NationalGrid
{
    public class SoapDownloader : ISoapDownloader
    {
        private static readonly XNamespace s_ns = XNamespace.Get("http://schemas.xmlsoap.org/soap/envelope/");

        public SoapDownloader(ITaskHelper taskHelper)
        {
            _taskHelper = taskHelper;
        }

        private ITaskHelper _taskHelper;

        public async Task<XElement> GetSoapAsync(
            string uri, string soapAction, string soapRequestBody, CancellationToken ct = default(CancellationToken))
        {
            var requestBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                $"<soap:Body>{soapRequestBody}</soap:Body>" +
                "</soap:Envelope>";
            var content = new ByteArrayContent(Encoding.UTF8.GetBytes(requestBody));
            content.Headers.Add("SOAPAction", soapAction);
            content.Headers.Add("Content-Type", "text/xml; charset=utf-8");
            using (var httpClient = new HttpClient())
            {
                var response = await httpClient.PostAsync(uri, content, ct).ConfigureAwait(_taskHelper);
                var bodyStream = await response.Content.ReadAsStreamAsync();
                var xDoc = XDocument.Load(bodyStream);
                return xDoc.Root.Element(s_ns + "Body").Elements().First();
            }
        }
    }
}

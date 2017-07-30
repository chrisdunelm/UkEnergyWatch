using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;

namespace Ukew.Elexon
{
    public class FakeElexonDownloader : IElexonDownloader
    {
        private static Assembly s_assembly = typeof(FakeElexonDownloader).GetTypeInfo().Assembly;

        public Task<XDocument> GetXmlAsync(string reportName, IDictionary<string, string> getParams, CancellationToken ct = default(CancellationToken))
        {
            var ps = getParams.OrderBy(x => x.Key).Aggregate("", (s, p) => $"{s}_{p.Key}_{p.Value}");
            var xmlStream = s_assembly.GetManifestResourceStream($"ukewlib_test.resources.elexon.{reportName}{ps}");
            return Task.FromResult(XDocument.Load(xmlStream));
        }
    }
}
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
            string CleanChars(string s) => new string(s.Select(c => char.IsLetterOrDigit(c) ? c : '_').ToArray());
            var ps = getParams.OrderBy(x => x.Key).Aggregate("", (s, p) => $"{s}_{CleanChars(p.Key)}_{CleanChars(p.Value)}");
            var resourceName = $"{reportName}{ps}";
            var xmlStream = s_assembly.GetManifestResourceStream($"ukewlib_test.resources.elexon.{resourceName}");
            if (xmlStream == null)
            {
                throw new InvalidOperationException($"Test resource missing: '{resourceName}'");
            }
            return Task.FromResult(XDocument.Load(xmlStream));
        }
    }
}
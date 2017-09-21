using System;
using System.Linq;
using System.Reflection;
using System.Xml.Linq;

namespace Ukew.Testing
{
    public static class Soap
    {
        private static readonly Assembly s_assembly = typeof(Soap).GetTypeInfo().Assembly;
        private static readonly XNamespace s_ns = XNamespace.Get("http://schemas.xmlsoap.org/soap/envelope/");

        public static XElement LoadBody(string soapResourceName)
        {
            var xmlStream = s_assembly.GetManifestResourceStream($"ukewlib_test.resources.soap.{soapResourceName}");
            var xDoc = XDocument.Load(xmlStream);
            return xDoc.Root.Element(s_ns + "Body").Elements().First();
        }
    }
}

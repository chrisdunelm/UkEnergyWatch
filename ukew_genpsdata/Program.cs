using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Xml.Linq;
using CommandLine;
using ExcelDataReader;
using Ukew.Elexon;
using UnitsNet;
using UnitsNet.Units;

namespace Ukew
{
    public class Options
    {
        [Option(Required = true, HelpText = "Path to the 'GB Electric EIC Library' file. Downloaded from 'http://www2.nationalgrid.com/UK/Industry-information/Europe/Local-issuing-office/'")]
        public string EicLibraryXlsPath { get; set; }

        [Option(Required = true, HelpText = "Path including filename of the generated C# code")]
        public string OutputCsPath { get; set; }
    }

    public static class Program
    {
        public static void Main(string[] args)
        {
            var cmdLineResult = Parser.Default.ParseArguments<Options>(args) as Parsed<Options>;
            if (cmdLineResult == null)
            {
                return;
            }
            var opts = cmdLineResult.Value;

            System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);

            var xDocs = LoadB1420Xml().ToList();
            var items = xDocs
                .SelectMany(doc =>
                    doc.Root.Element("responseBody").Element("responseList").Elements("item"))
                .Where(item => Value(item.Element("businessType")) == "Production unit");
            var xmlItemsByResourceName = new Dictionary<string, XElement>();
            foreach (var item in items)
            {
                // Done in a foreach, rather than .ToDictionary(), as there may be duplicates.
                // We want the later of the duplicates, so it's correct to replace them.
                xmlItemsByResourceName[Value(item.Element("registeredResourceName"))] = item;
            }

            var generationUnits = new List<EicIds.GenerationUnit>();
            var powerStations = new List<EicIds.PowerStation>();

            using (var xlsStream = File.OpenRead(opts.EicLibraryXlsPath))
            {
                using (var xlsReader = ExcelReaderFactory.CreateReader(xlsStream))
                {
                    string S(int col) => xlsReader.GetString(col);
                    // Generation Units
                    MoveToSheet(xlsReader, name => name == "ESO W Type Codes - BMUs");
                    AssertEquals(18, xlsReader.FieldCount);
                    xlsReader.Read();
                    // First row is headers. Check they are what we expect
                    AssertEquals("Display Name", S(0));
                    AssertEquals("STATION", S(1));
                    AssertEquals("Energy Identification Code", S(4));
                    AssertEquals("Function", S(5));
                    AssertEquals("EIC Name - Asset Name", S(6));
                    AssertEquals("EIC Responsible Party", S(8));
                    AssertEquals("BMRA_FUEL_TYPE", S(15));
                    AssertEquals("MAX_CAPACITY", S(16));
                    // Read all rows that have Function == "Generation Unit"
                    while (xlsReader.Read())
                    {
                        if (S(5) != "Generation Unit" || xlsReader.GetDouble(16) <= 0.0)
                        {
                            continue;
                        }
                        var s0 = S(0);
                        if (generationUnits.Any(x => x.RegisteredResourceName == s0))
                        {
                            continue;
                        }
                        var xml = xmlItemsByResourceName.TryGetValue(s0, out var xml1) ? xml1 : null;
                        var bmUnitId = Value(xml?.Element("bMUnitID"));
                        if (bmUnitId == "" || bmUnitId == "NA")
                        {
                            bmUnitId = null;
                        }
                        var maxCapacity = xlsReader.GetDouble(16);
                        var maxCapacityPower = Power.FromMegawatts(maxCapacity == 0.0 || maxCapacity == 999.0 ? (double?)null : maxCapacity);
                        generationUnits.Add(new EicIds.GenerationUnit(
                            registeredResourceName: s0,
                            powerStationRegisteredResourceName: S(1),
                            registeredResourceEic: S(4),
                            assetName: S(6),
                            responsibleParty: S(8),
                            fuelType: GetFuelType(S(15) ?? "", xml),
                            maxCapacity: maxCapacityPower,
                            bmUnitId: bmUnitId
                        ));
                    }
                    var genUnitsByPowerStation = generationUnits.ToLookup(x => x.PowerStationRegisteredResourceName);
                    // Power stations
                    MoveToSheet(xlsReader, name => name == "ESO W Type Codes - Stations");
                    AssertEquals(12, xlsReader.FieldCount);
                    xlsReader.Read();
                    // First row is headers. Check they are what we expect
                    AssertEquals("Display Name", S(0));
                    AssertEquals("Energy Identification Code", S(4));
                    AssertEquals("Function", S(5));
                    AssertEquals("EIC Name - Asset Name", S(6));
                    AssertEquals("EIC Responsible Party", S(7));
                    // Read all rows that have Function == "Production Unit"
                    while (xlsReader.Read())
                    {
                        if (S(5) != "Production Unit" || S(6).ToLowerInvariant().Contains("demand"))
                        {
                            continue;
                        }
                        var s0 = S(0);
                        if (powerStations.Any(x => x.RegisteredResourceName == s0))
                        {
                            continue;
                        }
                        var xml = xmlItemsByResourceName.TryGetValue(s0, out var xml1) ? xml1 : null;
                        var genUnits = genUnitsByPowerStation[s0];
                        var fuelType = genUnits
                            .Where(x => x.FuelType != EicIds.FuelType.Unknown)
                            .GroupBy(x => x.FuelType)
                            .OrderByDescending(x => x.Count())
                            .FirstOrDefault()?.Key ?? EicIds.FuelType.Unknown;
                        powerStations.Add(new EicIds.PowerStation(
                            registeredResourceName: s0,
                            registeredResourceEic: S(4),
                            assetName: S(6),
                            responsibleParty: S(7),
                            fuelType: fuelType,
                            installedCapacity: Power.FromMegawatts(ValueDoubleN(xml?.Element("nominal"))),
                            voltageLimit: ElectricPotential.FromKilovolts(ValueDoubleN(xml?.Element("voltageLimit"))),
                            generationUnitRegisteredResourceNames: genUnits.Select(x => x.RegisteredResourceName).ToArray()
                        ));
                    }
                }
            }

            //var genUnitsByStation = generationUnits.ToLookup(x => x.StationDisplayName);
            var generationUnitsByResourceName = generationUnits.ToDictionary(x => x.RegisteredResourceName);

            Console.WriteLine($"Station count: {powerStations.Count}");
            Console.WriteLine($"GenUnit Count: {generationUnits.Count}");

            using (var csWriter = File.CreateText(opts.OutputCsPath))
            {
                void W(string s) => csWriter.WriteLine(s);
                void W8(string s) => csWriter.WriteLine($"        {s}");
                W("using System.Collections.Generic;");
                W("using UnitsNet;");
                W("");
                W("// Auto-generated. DO NOT EDIT!");
                W("");
                W("namespace Ukew.Elexon");
                W("{");
                W("    public static partial class EicIds");
                W("    {");
                W8("public static List<PowerStation> PowerStations { get; } = new List<PowerStation>");
                W8("{");
                foreach (var ps in powerStations.OrderBy(x => x.RegisteredResourceName))
                {
                    var installedCapacity = ps.InstalledCapacity;
                    if (installedCapacity == null)
                    {
                        installedCapacity = ps.GenerationUnitRegisteredResourceNames
                            .Select(x => generationUnitsByResourceName[x])
                            .Aggregate(Power.Zero, (total, x) => total + x.MaxCapacity ?? Power.Zero);
                        if (installedCapacity.Value.Watts == 0.0)
                        {
                            installedCapacity = null;
                        }
                    }
                    W8($"    new PowerStation(\"{ps.RegisteredResourceName}\", \"{ps.RegisteredResourceEic}\", " +
                        $"\"{ps.AssetName}\", \"{ps.ResponsibleParty}\", FuelType.{ps.FuelType}, " +
                        ((installedCapacity is Power p) ? $"Power.FromMegawatts({p.Megawatts})" : "null" ) + ", " +
                        ((ps.VoltageLimit is ElectricPotential e) ? $"ElectricPotential.FromKilovolts({e.Kilovolts})" : "null") + ", " +
                        $"new string[] {{ {string.Join(", ", ps.GenerationUnitRegisteredResourceNames.Select(x => $"\"{x}\""))} }}),");
                }
                W8("};");
                W8("");
                W8("public static List<GenerationUnit> GenerationUnits { get; } = new List<GenerationUnit>");
                W8("{");
                foreach (var gu in generationUnits.OrderBy(x => x.RegisteredResourceName))
                {
                    W8($"    new GenerationUnit(\"{gu.RegisteredResourceName}\", \"{gu.PowerStationRegisteredResourceName}\", " +
                        $"\"{gu.RegisteredResourceEic}\", \"{gu.AssetName}\", \"{gu.ResponsibleParty}\", " +
                        $"FuelType.{gu.FuelType}, " +
                        ((gu.MaxCapacity is Power p) ? $"Power.FromMegawatts({p.Megawatts})" : "null") + ", " +
                        (gu.BmUnitId == null ? "null" : $"\"{gu.BmUnitId}\"") + "),");
                }
                W8("};");
                W("    }");
                W("}");
            }
        }

        private static void AssertEquals<T>(T expected, T actual)
        {
            if (!expected.Equals(actual))
            {
                throw new Exception($"Expected '{expected}', but actually '{actual}'");

            }
        }

        private static void MoveToSheet(IExcelDataReader xlsReader, Func<string, bool> namePred)
        {
            xlsReader.Reset();
            while (!namePred(xlsReader.Name))
            {
                if (!xlsReader.NextResult())
                {
                    throw new Exception("Cannot find sheet");
                }
            }
        }

        private static EicIds.FuelType GetFuelType(string bmraFuelType, XElement xmlItem)
        {
            // The following from the EIC xls are fine
            switch (bmraFuelType.ToUpperInvariant())
            {
                case "COAL": return EicIds.FuelType.Coal;
                case "OCGT": return EicIds.FuelType.Ocgt;
                case "CCGT": return EicIds.FuelType.Ccgt;
                case "OIL": return EicIds.FuelType.Oil;
                case "NUCLEAR": return EicIds.FuelType.Nuclear;
                case "PS": return EicIds.FuelType.PumpedStorage;
                case "NPSHYD": return EicIds.FuelType.Hydro;
            }
            // If none of the above, look in the B1420 data if we have it
            if (xmlItem != null)
            {
                var resourceType = Value(xmlItem.Element("powerSystemResourceType")).Trim('"');
                switch (resourceType.ToLowerInvariant())
                {
                    case "wind onshore": return EicIds.FuelType.WindOnshore;
                    case "wind offshore": return EicIds.FuelType.WindOffshore;
                }
            }
            // Finally fallback...
            switch (bmraFuelType.ToUpperInvariant())
            {
                case "WIND": return EicIds.FuelType.Wind;
                case "": return EicIds.FuelType.Unknown;
                case "OTHER": return EicIds.FuelType.Other;
            }
            throw new Exception($"Cannot determine fuel type. bmraFuelType='{bmraFuelType}'");
        }

        private static IEnumerable<XDocument> LoadB1420Xml()
        {
            for (int year = 1998; year <= 2016; year += 1)
            {
                var xmlStream = typeof(Program).GetTypeInfo().Assembly.GetManifestResourceStream($"ukew_genpsdata.resources.B1420_Year_{year}");
                var xDoc = XDocument.Load(xmlStream);
                if (xDoc.Root.Element("responseMetadata").Element("httpCode").Value.Trim() == "200")
                {
                    yield return xDoc;
                }
            }
        }

        private static string Value(XElement x)
        {
            if (x == null)
            {
                return null;
            }
            var s = x.Value.Trim();
            s = s.Replace('\r', ' ').Replace('\n', ' ');
            while (s.Contains("  "))
            {
                s = s.Replace("  ", " ");
            }
            return s;
        }

        private static double? ValueDoubleN(XElement x)
        {
            if (x == null)
            {
                return null;
            }
            var d = double.Parse(Value(x));
            return d == 0.0 ? (double?)null : d;
        }
    }
}
using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;
using NodaTime;
using NodaTime.Text;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using UnitsNet;

namespace Ukew.NationalGrid
{
    public class InstantaneousFlow
    {
        public class Reader : DataStoreReader<Data, Data>
        {
            public Reader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "instantaneousflow")
            {
                Strings = new Strings(taskHelper, dir);
            }

            public Strings Strings { get; }
        }

        public class Writer : DataStoreWriter<Data, Data>
        {
            public Writer(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "instantaneousflow") { }
        }

        public enum SupplyType : byte
        {
            ZoneSupply = 1,
            TerminalSupply = 2,
            TotalSupply = 3,
        }

        public struct Data : IStorable<Data, Data>, IStorableFactory<Data>, IEquatable<Data>, IStringMap<Data>
        {
            public Data(Instant update, SupplyType type, ushort nameIndex, Flow flowRate)
                : this((uint)update.ToUnixTimeSeconds(), (byte)type, nameIndex, (uint)(flowRate.CubicMetersPerHour * 24.0)) { }

            private Data(uint updateUnixSeconds, byte type, ushort nameIndex, uint flowRateCubicMetresPerDay)
            {
                _updateUnixSeconds = updateUnixSeconds;
                _type = type;
                _nameIndex = nameIndex;
                _flowRateCubicMetresPerDay = flowRateCubicMetresPerDay;
            }

            private readonly uint _updateUnixSeconds;
            private readonly byte _type;
            private readonly ushort _nameIndex;
            private readonly uint _flowRateCubicMetresPerDay;

            public Instant Update => Instant.FromUnixTimeSeconds(_updateUnixSeconds);
            public SupplyType Type => (SupplyType)_type;
            public ushort NameIndex => _nameIndex;
            public Flow FlowRate => Flow.FromCubicMetersPerHour(((double)_flowRateCubicMetresPerDay) / 24.0);

            public Task<string> NameAsync(Strings strings) => strings.GetAsync(_nameIndex);

            Data IStringMap<Data>.CloneWithStrings(IReadOnlyList<long> indexes) =>
                new Data(_updateUnixSeconds, _type, (ushort)indexes[0], _flowRateCubicMetresPerDay);

            int IStorableFactory<Data>.CurrentVersion => 1;

            ImmutableArray<byte> IStorableFactory<Data>.Store(Data item) => Bits.Empty
                .AddUInt(item._updateUnixSeconds)
                .AddByte(item._type)
                .AddUShort(item._nameIndex)
                .AddUInt(item._flowRateCubicMetresPerDay);

            Data IStorableFactory<Data>.Load(int version, ImmutableArray<byte> bytes)
            {
                switch (version)
                {
                    case 1:
                        return new Data(Bits.GetUInt(bytes, 0), Bits.GetByte(bytes, 4), Bits.GetUShort(bytes, 5), Bits.GetUInt(bytes, 7));
                    default:
                        throw new Exception($"Unknown version: {version}");
                }
            }

            public static bool operator ==(Data a, Data b) =>
                a._updateUnixSeconds == b._updateUnixSeconds && a._type == b._type && a._nameIndex == b._nameIndex &&
                a._flowRateCubicMetresPerDay == b._flowRateCubicMetresPerDay;
            public override int GetHashCode() => (int)(_updateUnixSeconds ^ _type ^ _nameIndex ^ _flowRateCubicMetresPerDay);

            public override bool Equals(object obj) => (obj is Data other) && this == other;
            public bool Equals(Data other) => this == other;
            public static bool operator !=(Data a, Data b) => !(a == b);

            public override string ToString() => $"{{ Update:{Update}, Type:{Type}, NameIndex:{NameIndex}, Flow:{FlowRate} }}";

            public string ToString(Strings strings) =>
                $"{{ Update:{Update}, Type:{Type}, Name:{NameAsync(strings).Result} (index:{NameIndex}), Flow:{FlowRate} }}";
        }

        private static readonly XNamespace s_ns = XNamespace.Get("http://www.NationalGrid.com/EDP/UI/");
        private static readonly XNamespace s_nsEntities = XNamespace.Get("http://www.NationalGrid.com/EDP/BusinessEntities/Public");
        private static readonly LocalDateTimePattern s_dateTimePattern = LocalDateTimePattern.CreateWithInvariantCulture("uuuu'-'MM'-'dd'T'HH':'mm':'ss");
        private static readonly Regex s_compactWhitespaceRx = new Regex("\\s+");
        private static readonly DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

        public InstantaneousFlow(ITaskHelper taskHelper, ISoapDownloader soapDownloader)
        {
            _taskHelper = taskHelper;
            _soapDownloader = soapDownloader;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly ISoapDownloader _soapDownloader;

        private string CompactWhitespace(string s) => s_compactWhitespaceRx.Replace(s, " ").Trim();

        private Instant LocalToInstant(string dateTime) =>
            s_dateTimePattern.Parse(dateTime).Value.InZoneLeniently(s_tzLondon).ToInstant();

        public async Task<Instant> GetLatestPublicationTimeAsync(CancellationToken ct = default(CancellationToken))
        {
            var uri = "http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx";
            var soapAction = "http://www.NationalGrid.com/EDP/UI/GetLatestPublicationTime";
            var requestSoapBody = "<GetLatestPublicationTime xmlns=\"http://www.NationalGrid.com/EDP/UI/\"/>";
            var responseSoapBody = await _soapDownloader.GetSoapAsync(uri, soapAction, requestSoapBody, ct);
            var dateTime = CompactWhitespace(responseSoapBody.Element(s_ns + "GetLatestPublicationTimeResult").Value);
            return LocalToInstant(dateTime);
        }

        public async Task<IReadOnlyList<Strings.Map<Data>>> GetInstantaneousFlowDataAsync(CancellationToken ct = default(CancellationToken))
        {
            var uri = "http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx";
            var soapAction = "http://www.NationalGrid.com/EDP/UI/GetInstantaneousFlowData";
            var requestSoapBody = "<GetInstantaneousFlowData xmlns=\"http://www.NationalGrid.com/EDP/UI/\" />";
            var responseSoapBody = await _soapDownloader.GetSoapAsync(uri, soapAction, requestSoapBody, ct);
            var collection = responseSoapBody
                .Element(s_ns + "GetInstantaneousFlowDataResult")
                .Element(s_nsEntities + "EDPReportPage")
                .Element(s_nsEntities + "EDPEnergyGraphTableCollection");
            SupplyType GetSupplyType(XElement graphTableEl)
            {
                switch (CompactWhitespace(graphTableEl.Element(s_nsEntities + "Description").Value).ToLowerInvariant())
                {
                    case "zone supply": return SupplyType.ZoneSupply;
                    case "terminal supply": return SupplyType.TerminalSupply;
                    case "total supply": return SupplyType.TotalSupply;
                    default: throw new ArgumentOutOfRangeException(nameof(graphTableEl));
                }
            }
            return collection.Elements(s_nsEntities + "EDPEnergyGraphTableBE")
                .SelectMany(graphTableEl =>
                {
                    var supplyType = GetSupplyType(graphTableEl);
                    return graphTableEl.Element(s_nsEntities + "EDPObjectCollection").Elements(s_nsEntities + "EDPObjectBE")
                        .Select(obj => (supplyType, obj));
                })
                .SelectMany(x =>
                {
                    var objName = CompactWhitespace(x.obj.Element(s_nsEntities + "EDPObjectName").Value);
                    return x.obj.Element(s_nsEntities + "EnergyDataList").Elements(s_nsEntities + "EDPEnergyDataBE")
                        .Select(energy => (x.supplyType, objName, energy));
                })
                .Select(x =>
                {
                    var applicableAt = CompactWhitespace(x.energy.Element(s_nsEntities + "ApplicableAt").Value);
                    var update = LocalToInstant(applicableAt);
                    var flowRate = CompactWhitespace(x.energy.Element(s_nsEntities + "FlowRate").Value);
                    var flow = Flow.FromCubicMetersPerHour(double.Parse(flowRate) * 1e6 / 24);
                    var data = new Data(update, x.supplyType, 0, flow);
                    return new Strings.Map<Data>(data, x.objName);
                })
                .ToList();
        }
    }
}

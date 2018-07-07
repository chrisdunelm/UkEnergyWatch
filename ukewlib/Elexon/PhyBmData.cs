using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using NodaTime;
using NodaTime.Text;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using UnitsNet;

namespace Ukew.Elexon
{
    public class PhyBmData
    {
        public class FpnReader : DataStoreReader<FpnData, FpnData>
        {
            public FpnReader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "fpndata") { }
        }

        public class FpnWriter : DataStoreWriter<FpnData, FpnData>
        {
            public FpnWriter(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "fpndata") { }
        }

        public readonly struct FpnData : IStorable<FpnData, FpnData>, IStorableFactory<FpnData>, IEquatable<FpnData>
        {
            public FpnData(string resourceName, Instant timeFrom, Power levelFrom, Instant timeTo, Power levelTo)
                : this(EicIds.Hash(resourceName), (uint)timeFrom.ToUnixTimeSeconds(), (short)levelFrom.Megawatts,
                    (uint)timeTo.ToUnixTimeSeconds(), (short)levelTo.Megawatts) { }

            private FpnData(uint resourceNameHash, uint timeFromUnixSeconds, short levelFromMw, uint timeToUnixSeconds, short levelToMw)
            {
                _resourceNameHash = resourceNameHash;
                _timeFromUnixSeconds = timeFromUnixSeconds;
                _levelFromMw = levelFromMw;
                _timeToUnixSeconds = timeToUnixSeconds;
                _levelToMw = levelToMw;
            }

            private readonly uint _resourceNameHash;
            private readonly uint _timeFromUnixSeconds;
            private readonly short _levelFromMw;
            private readonly uint _timeToUnixSeconds;
            private readonly short _levelToMw;

            public uint ResourceNameHash => _resourceNameHash;
            public string ResourceName => EicIds.LookupResourceNameHash(_resourceNameHash)?.RegisteredResourceName ?? $"0x{ResourceNameHash:x8}";
            public Instant TimeFrom => Instant.FromUnixTimeSeconds(_timeFromUnixSeconds);
            public Power LevelFrom => Power.FromMegawatts(_levelFromMw);
            public Instant TimeTo => Instant.FromUnixTimeSeconds(_timeToUnixSeconds);
            public Power LevelTo => Power.FromMegawatts(_levelToMw);

            public Power? LevelAt(Instant t)
            {
                if (t >= TimeFrom && t < TimeTo)
                {
                    return LevelFrom + ((LevelTo - LevelFrom) * ((t - TimeFrom) / (TimeTo - TimeFrom)));
                }
                else
                {
                    return null;
                }
            }

            int IStorableFactory<FpnData>.CurrentVersion => 1;

            ImmutableArray<byte> IStorableFactory<FpnData>.Store(FpnData item) => Bits.Empty
                .AddUInt(item._resourceNameHash)
                .AddUInt(item._timeFromUnixSeconds)
                .AddShort(item._levelFromMw)
                .AddUInt(item._timeToUnixSeconds)
                .AddShort(item._levelToMw);

            FpnData IStorableFactory<FpnData>.Load(int version, ReadOnlySpan<byte> bytes)
            {
                switch (version)
                {
                    case 1:
                        return new FpnData(Bits.GetUInt(bytes, 0),
                            Bits.GetUInt(bytes, 4), Bits.GetShort(bytes, 8), Bits.GetUInt(bytes, 10), Bits.GetShort(bytes, 14));
                    default:
                        throw new Exception($"Unknown version: {version}");
                }
            }

            public static bool operator ==(FpnData a, FpnData b) =>
                a._resourceNameHash == b._resourceNameHash && a._timeFromUnixSeconds == b._timeFromUnixSeconds &&
                a._levelFromMw == b._levelFromMw && a._timeToUnixSeconds == b._timeToUnixSeconds && a._levelToMw == b._levelToMw;
            public override int GetHashCode() =>
                (int)(_resourceNameHash ^ _timeFromUnixSeconds ^ _levelFromMw ^ _timeToUnixSeconds ^ ((int)_levelToMw) << 16);

            public override bool Equals(object obj) => (obj is FpnData other) && this == other;
            public bool Equals(FpnData other) => this == other;
            public static bool operator !=(FpnData a, FpnData b) => !(a == b);

            public override string ToString() =>
                $"{{ ResourceName:{ResourceName} (0x{ResourceNameHash:x8}), TimeFrom:{TimeFrom}, LevelFrom:{LevelFrom}, TimeTo:{TimeTo}, LevelTo:{LevelTo} }}";
        }

        public PhyBmData(ITaskHelper taskHelper, IElexonDownloader downloader)
        {
            _taskHelper = taskHelper;
            _downloader = downloader;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IElexonDownloader _downloader;

        private static readonly InstantPattern s_timePattern = InstantPattern.CreateWithInvariantCulture("yyyy'-'MM'-'dd' 'HH':'mm':'ss");

        public async Task<IReadOnlyList<FpnData>> GetAsync(
            LocalDate settlementDate, int settlementPeriod, CancellationToken ct = default(CancellationToken))
        {
            var param = new Dictionary<string, string>
            {
                { "SettlementDate", settlementDate.ToString("yyyy'-'MM'-'dd", null) },
                { "SettlementPeriod", settlementPeriod.ToString() }
            };
            var xDoc = await _downloader.GetXmlAsync("PHYBMDATA", param, ct).ConfigureAwait(_taskHelper);
            var httpCode = xDoc.Root.Element("responseMetadata").Element("httpCode").Value.Trim();
            switch (httpCode)
            {
                case "204":
                    return new FpnData[0];
                case "200":
                    var responseList = xDoc.Root.Element("responseBody").Element("responseList");
                    var fpnItems = responseList.Elements("item").Where(item => item.Element("recordType").Value.Trim() == "PN");
                    return fpnItems.Select(item =>
                    {
                        var resourceName = item.Element("ngcBMUnitName").Value.Trim();
                        var timeFrom = s_timePattern.Parse(item.Element("timeFrom").Value.Trim()).Value;
                        var levelFrom = Power.FromMegawatts(double.Parse(item.Element("pnLevelFrom").Value.Trim()));
                        var timeTo = s_timePattern.Parse(item.Element("timeTo").Value.Trim()).Value;
                        var levelTo = Power.FromMegawatts(double.Parse(item.Element("pnLevelTo").Value.Trim()));
                        return new FpnData(resourceName, timeFrom, levelFrom, timeTo, levelTo);
                    }).ToImmutableList();
                default:
                    throw new InvalidOperationException($"Bad data. HTTP code = {httpCode}");
            }
        }
    }
}

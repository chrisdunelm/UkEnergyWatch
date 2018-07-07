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
    // B1610 – Actual Generation Output per Generation Unit
    public class B1610
    {
        public class Reader : DataStoreReader<Data, Data>
        {
            public Reader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "b1610") { }
        }

        public class Writer : DataStoreWriter<Data, Data>
        {
            public Writer(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "b1610") { }
        }

        public readonly struct Data : IStorable<Data, Data>, IStorableFactory<Data>, IEquatable<Data>
        {
            public Data(string resourceName, Instant settlementPeriodStart, Power power)
                : this(EicIds.Hash(resourceName), (uint)settlementPeriodStart.ToUnixTimeSeconds(), (int)power.Kilowatts) { }

            private Data(uint resourceNameHash, uint settlementPeriodStartUnixSeconds, int powerKw)
            {
                _resourceNameHash = resourceNameHash;
                _settlementPeriodStartUnixSeconds = settlementPeriodStartUnixSeconds;
                _powerKw = powerKw;
            }

            private readonly uint _resourceNameHash;
            private readonly uint _settlementPeriodStartUnixSeconds;
            private readonly int _powerKw;

            public uint ResourceNameHash => _resourceNameHash;
            public string ResourceName => EicIds.LookupResourceNameHash(_resourceNameHash)?.RegisteredResourceName ?? $"0x{ResourceNameHash:x8}";
            public Instant SettlementPeriodStart => Instant.FromUnixTimeSeconds(_settlementPeriodStartUnixSeconds);
            public Power Power => Power.FromKilowatts(_powerKw);

            int IStorableFactory<Data>.CurrentVersion => 1;

            ImmutableArray<byte> IStorableFactory<Data>.Store(Data item) => Bits.Empty
                .AddUInt(item._resourceNameHash)
                .AddUInt(item._settlementPeriodStartUnixSeconds)
                .AddInt(item._powerKw);

            Data IStorableFactory<Data>.Load(int version, ReadOnlySpan<byte> bytes)
            {
                switch (version)
                {
                    case 1:
                        return new Data(Bits.GetUInt(bytes, 0), Bits.GetUInt(bytes, 4), Bits.GetInt(bytes, 8));
                    default:
                        throw new Exception($"Unknown version: {version}");
                }
            }

            public static bool operator ==(Data a, Data b) =>
                a._resourceNameHash == b._resourceNameHash && a._settlementPeriodStartUnixSeconds == b._settlementPeriodStartUnixSeconds &&
                a._powerKw == b._powerKw;
            public override int GetHashCode() => (int)(_resourceNameHash ^ _settlementPeriodStartUnixSeconds ^ _powerKw);

            public override bool Equals(object obj) => (obj is Data other) && this == other;
            public bool Equals(Data other) => this == other;
            public static bool operator !=(Data a, Data b) => !(a == b);

            public override string ToString() =>
            $"{{ ResourceName:{ResourceName} (0x{ResourceNameHash:x8}), SettlementPeriodStart:{SettlementPeriodStart}, Power:{Power} }}";
        }

        public B1610(ITaskHelper taskHelper, IElexonDownloader downloader)
        {
            _taskHelper = taskHelper;
            _downloader = downloader;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IElexonDownloader _downloader;

        private static readonly LocalDatePattern s_datePattern = LocalDatePattern.CreateWithInvariantCulture("yyyy'-'MM'-'dd");
        
        public async Task<IReadOnlyList<Data>> GetAsync(
            LocalDate settlementDate, int settlementPeriod, CancellationToken ct = default(CancellationToken))
        {
            var param = new Dictionary<string, string>
            {
                { "SettlementDate", settlementDate.ToString("yyyy'-'MM'-'dd", null) },
                { "Period", settlementPeriod.ToString() }
            };
            var xDoc = await _downloader.GetXmlAsync("B1610", param, ct).ConfigureAwait(_taskHelper);
            var httpCode = xDoc.Root.Element("responseMetadata").Element("httpCode").Value.Trim();
            switch (httpCode)
            {
                case "204":
                    return new Data[0];
                case "200":
                    var items = xDoc.Root.Element("responseBody").Element("responseList").Elements("item");
                    return items.Select(item =>
                    {
                        var resourceName = item.Element("nGCBMUnitID").Value.Trim();
                        var itemSettlementDate = s_datePattern.Parse(item.Element("settlementDate").Value.Trim()).Value;
                        var itemSettlementPeriod = int.Parse(item.Element("settlementPeriod").Value.Trim());
                        var power = Power.FromMegawatts(double.Parse(item.Element("quantity").Value.Trim()));
                        var settlementPeriodStart = (itemSettlementDate, itemSettlementPeriod).SettlementPeriodStart();
                        return new Data(resourceName, settlementPeriodStart, power);
                    }).ToImmutableList();
                default:
                    throw new InvalidOperationException($"Bad data. HTTP code = {httpCode}");
            }
        }
    }
}

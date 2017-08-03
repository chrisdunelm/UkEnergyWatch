using System;
using System.Collections.Generic;
using System.Collections.Immutable;
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
    public class FuelInstHhCur
    {
        public class Reader : DataStoreReader<Data, Data>
        {
            public Reader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir) { }
        }

        public class Writer : DataStoreWriter<Data, Data>
        {
            public Writer(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir) { }
        }

        public struct Data : IStorable<Data, Data>, IStorableFactory<Data>, IEquatable<Data>
        {
            public Data(
                Instant update,
                Power ccgt,
                Power ocgt,
                Power oil,
                Power coal,
                Power nuclear,
                Power wind,
                Power ps,
                Power npshyd,
                Power other,
                Power intFr,
                Power intIrl,
                Power intNed,
                Power intEw
            ) : this(
                (uint)update.ToUnixTimeSeconds(),
                (ushort)ccgt.Megawatts,
                (ushort)ocgt.Megawatts,
                (ushort)oil.Megawatts,
                (ushort)coal.Megawatts,
                (ushort)nuclear.Megawatts,
                (ushort)wind.Megawatts,
                (ushort)ps.Megawatts,
                (ushort)npshyd.Megawatts,
                (ushort)other.Megawatts,
                (ushort)intFr.Megawatts,
                (ushort)intIrl.Megawatts,
                (ushort)intNed.Megawatts,
                (ushort)intEw.Megawatts
            ) { }

            private Data(
                uint updateUnixSeconds,
                ushort ccgtMw,
                ushort ocgtMw,
                ushort oilMw,
                ushort coalMw,
                ushort nuclearMw,
                ushort windMw,
                ushort psMw,
                ushort npshydMw,
                ushort otherMw,
                ushort intFrMw,
                ushort intIrlMw,
                ushort intNedMw,
                ushort intEwMw
            )
            {
                _updateUnixSeconds = updateUnixSeconds;
                _ccgtMw = ccgtMw;
                _ocgtMw = ocgtMw;
                _oilMw = oilMw;
                _coalMw = coalMw;
                _nuclearMw = nuclearMw;
                _windMw = windMw;
                _psMw = psMw;
                _npshydMw = npshydMw;
                _otherMw = otherMw;
                _intFrMw = intFrMw;
                _intIrlMw = intIrlMw;
                _intNedMw = intNedMw;
                _intEwMw = intEwMw;
            }

            private readonly uint _updateUnixSeconds;
            private readonly ushort _ccgtMw;
            private readonly ushort _ocgtMw;
            private readonly ushort _oilMw;
            private readonly ushort _coalMw;
            private readonly ushort _nuclearMw;
            private readonly ushort _windMw;
            private readonly ushort _psMw;
            private readonly ushort _npshydMw;
            private readonly ushort _otherMw;
            private readonly ushort _intFrMw;
            private readonly ushort _intIrlMw;
            private readonly ushort _intNedMw;
            private readonly ushort _intEwMw;

            public Instant Update => Instant.FromUnixTimeSeconds(_updateUnixSeconds);
            public Power Ccgt => Power.FromMegawatts(_ccgtMw);
            public Power Ocgt => Power.FromMegawatts(_ocgtMw);
            public Power Oil => Power.FromMegawatts(_oilMw);
            public Power Coal => Power.FromMegawatts(_coalMw);
            public Power Nuclear => Power.FromMegawatts(_nuclearMw);
            public Power Wind => Power.FromMegawatts(_windMw);
            public Power Ps => Power.FromMegawatts(_psMw);
            public Power Npshyd => Power.FromMegawatts(_npshydMw);
            public Power Other => Power.FromMegawatts(_otherMw);
            public Power IntFr => Power.FromMegawatts(_intFrMw);
            public Power IntIrl => Power.FromMegawatts(_intIrlMw);
            public Power IntNed => Power.FromMegawatts(_intNedMw);
            public Power IntEw => Power.FromMegawatts(_intEwMw);

            public Power Total => Ccgt + Ocgt + Oil + Coal + Nuclear + Wind + Ps + Npshyd + Other + IntFr + IntIrl + IntNed + IntEw;

            int IStorableFactory<Data>.CurrentVersion => 1;

            Data IStorableFactory<Data>.Load(int version, ImmutableArray<byte> bytes)
            {
                switch (version)
                {
                    case 1:
                        return new Data(
                            Bits.GetUInt(bytes, 0),
                            Bits.GetUShort(bytes, 4),
                            Bits.GetUShort(bytes, 6),
                            Bits.GetUShort(bytes, 8),
                            Bits.GetUShort(bytes, 10),
                            Bits.GetUShort(bytes, 12),
                            Bits.GetUShort(bytes, 14),
                            Bits.GetUShort(bytes, 16),
                            Bits.GetUShort(bytes, 18),
                            Bits.GetUShort(bytes, 20),
                            Bits.GetUShort(bytes, 22),
                            Bits.GetUShort(bytes, 24),
                            Bits.GetUShort(bytes, 26),
                            Bits.GetUShort(bytes, 28)
                        );
                    default:
                        throw new Exception($"Unknown version: {version}");
                }
            }

            ImmutableArray<byte> IStorableFactory<Data>.Store(Data item) => Bits.Empty
                .Add(item._updateUnixSeconds)
                .Add(item._ccgtMw)
                .Add(item._ocgtMw)
                .Add(item._oilMw)
                .Add(item._coalMw)
                .Add(item._nuclearMw)
                .Add(item._windMw)
                .Add(item._psMw)
                .Add(item._npshydMw)
                .Add(item._otherMw)
                .Add(item._intFrMw)
                .Add(item._intIrlMw)
                .Add(item._intNedMw)
                .Add(item._intEwMw);

            public static bool operator ==(Data a, Data b) =>
                a._updateUnixSeconds == b._updateUnixSeconds &&
                a._ccgtMw == b._ccgtMw &&
                a._ocgtMw == b._ocgtMw &&
                a._oilMw == b._oilMw &&
                a._coalMw == b._coalMw &&
                a._nuclearMw == b._nuclearMw &&
                a._windMw == b._windMw &&
                a._psMw == b._psMw &&
                a._npshydMw == b._npshydMw &&
                a._otherMw == b._otherMw &&
                a._intFrMw == b._intFrMw &&
                a._intIrlMw == b._intIrlMw &&
                a._intNedMw == b._intNedMw &&
                a._intEwMw == b._intEwMw;
            public override int GetHashCode() => (int)_updateUnixSeconds ^
                _ccgtMw ^ _ocgtMw ^ _oilMw ^ _coalMw ^ _nuclearMw ^ _windMw ^ _psMw ^ _npshydMw ^ _otherMw ^
                _intFrMw ^ _intIrlMw ^ _intNedMw ^ _intEwMw;

            public override bool Equals(object obj) => (obj is Data other) && this == other;
            public bool Equals(Data other) => this == other;
            public static bool operator !=(Data a, Data b) => !(a == b);
        }

        public FuelInstHhCur(ITaskHelper taskHelper, IElexonDownloader downloader)
        {
            _taskHelper = taskHelper;
            _downloader = downloader;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IElexonDownloader _downloader;

        private static readonly InstantPattern s_updatePattern = InstantPattern.CreateWithInvariantCulture("yyyy'-'MM'-'dd' 'HH':'mm':'ss");
        public async Task<Data> GetAsync(CancellationToken ct = default(CancellationToken))
        {
            var xDoc = await _downloader.GetXmlAsync("FUELINSTHHCUR", new Dictionary<string, string>(), ct).ConfigureAwait(_taskHelper);
            var responseBody = xDoc.Root.Element("responseBody");
            var powers = responseBody.Element("responseList").Elements("item").ToImmutableDictionary(
                x => x.Element("fuelType").Value.Trim(), x => Power.FromMegawatts(int.Parse(x.Element("currentMW").Value.Trim())));
            return new Data(
                update: s_updatePattern.Parse(responseBody.Element("dataLastUpdated").Value.Trim()).Value,
                ccgt: powers["CCGT"],
                ocgt: powers["OCGT"],
                oil: powers["OIL"],
                coal: powers["COAL"],
                nuclear: powers["NUCLEAR"],
                wind: powers["WIND"],
                ps: powers["PS"],
                npshyd: powers["NPSHYD"],
                other: powers["OTHER"],
                intFr: powers["INTFR"],
                intIrl: powers["INTIRL"],
                intNed: powers["INTNED"],
                intEw: powers["INTEW"]
            );
        }
    }
}

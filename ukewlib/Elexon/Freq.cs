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
    public class Freq
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
            public Data(Instant update, Frequency frequency) : this(
                (uint)update.ToUnixTimeSeconds(), (uint)(frequency.Hertz * 1e6)
            ) { }

            private Data(uint updateUnixSeconds, uint freqMicroHz)
            {
                _updateUnixSeconds = updateUnixSeconds;
                _freqMicroHz = freqMicroHz;
            }

            private readonly uint _updateUnixSeconds;
            private readonly uint _freqMicroHz;

            public Instant Update => Instant.FromUnixTimeSeconds(_updateUnixSeconds);
            public Frequency Frequency => Frequency.FromHertz(((double)_freqMicroHz) * 1e-6);

            int IStorableFactory<Data>.CurrentVersion => 1;

            ImmutableArray<byte> IStorableFactory<Data>.Store(Data item) => Bits.Empty
                .AddUInt(item._updateUnixSeconds)
                .AddUInt(item._freqMicroHz);

            Data IStorableFactory<Data>.Load(int version, ImmutableArray<byte> bytes)
            {
                switch (version)
                {
                    case 1:
                        return new Data(Bits.GetUInt(bytes, 0), Bits.GetUInt(bytes, 4));
                    default:
                        throw new Exception($"Unknown version: {version}");
                }
            }

            public static bool operator ==(Data a, Data b) =>
                a._freqMicroHz == b._freqMicroHz && a._updateUnixSeconds == b._updateUnixSeconds;
            public override int GetHashCode() => (int)(_freqMicroHz ^ _updateUnixSeconds);

            public override bool Equals(object obj) => (obj is Data other) && this == other;
            public bool Equals(Data other) => this == other;
            public static bool operator !=(Data a, Data b) => !(a == b);

            public override string ToString() => $"{{ Update:{Update}, Frequency:{Frequency} }}";
        }

        public Freq(ITaskHelper taskHelper, IElexonDownloader downloader)
        {
            _taskHelper = taskHelper;
            _downloader = downloader;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly IElexonDownloader _downloader;

        private static readonly string s_dateFormatString = "yyyy'-'MM'-'dd' 'HH':'mm':'ss";
        private static readonly InstantPattern s_updatePattern = InstantPattern.CreateWithInvariantCulture("yyyy'-'MM'-'dd' 'HH':'mm':'ss");
        // From and To times are both inclusive
        public async Task<IReadOnlyList<Data>> GetAsync(Instant from, Instant to, CancellationToken ct = default(CancellationToken))
        {
            var param = new Dictionary<string, string>
            {
                { "FromDateTime", from.ToString(s_dateFormatString, null) },
                { "ToDateTime", to.ToString(s_dateFormatString, null) }
            };
            var xDoc = await _downloader.GetXmlAsync("FREQ", param, ct).ConfigureAwait(_taskHelper);
            var httpCode = xDoc.Root.Element("responseMetadata").Element("httpCode").Value.Trim();
            switch (httpCode)
            {
                case "204":
                    return new Data[0];
                case "200":
                    var responseList = xDoc.Root.Element("responseBody").Element("responseList");
                    return responseList.Elements("item").Select(item =>
                    {
                        var date = item.Element("reportSnapshotTime").Value.Trim();
                        var time = item.Element("spotTime").Value.Trim();
                        var update = s_updatePattern.Parse($"{date} {time}").Value;
                        var frequency = Frequency.FromHertz(double.Parse(item.Element("frequency").Value.Trim()));
                        return new Data(update, frequency);
                    }).ToImmutableList();
                default:
                    throw new InvalidOperationException($"Bad data. HTTP code = {httpCode}");
            }
        }
    }
}

using System;
using System.Collections.Immutable;
using System.Linq;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class DataFactory : IStorableFactory<Data>
    {
        public int CurrentVersion => 3;

        public Data Load(int version, ImmutableArray<byte> bytes)
        {
            switch (version)
            {
                case 1: return new Data(Bits.GetInt(bytes, 0), null, null);
                case 2: return new Data(Bits.GetInt(bytes, 0), Bits.GetIntN(bytes, 4), null);
                case 3: return new Data(Bits.GetInt(bytes, 0), Bits.GetIntN(bytes, 4), Bits.GetIntN(bytes, 9));
                default: throw new InvalidOperationException($"Invalid version: {version}");
            }
        }

        public ImmutableArray<byte> Store(Data item) => Bits.Empty
                .AddInt(item.A)
                .AddIntN(item.B)
                .AddIntN(item.C);
    }

    public class Data : IStorable<Data, DataFactory>, IEquatable<Data>
    {
        public class Reader : DataStoreReader<Data, DataFactory>
        {
            public Reader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir) { }
        }

        public class Writer : DataStoreWriter<Data, DataFactory>
        {
            public Writer(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir) { }
        }

        public Data(int a, int? b, int? c) => (A, B, C) = (a, b, c);
        public int A { get; } // Versions 1, 2, 3
        public int? B { get; } // Versions 2, 3
        public int? C { get; } // Version 3 only

        public override int GetHashCode() => A.GetHashCode() ^ B.GetHashCode() ^ C.GetHashCode();
        public override bool Equals(object other) => Equals(other as Data);
        public bool Equals(Data other) => other != null && A == other.A && B == other.B && C == other.C;
    }
}

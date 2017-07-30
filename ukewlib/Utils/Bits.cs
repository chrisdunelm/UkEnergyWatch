using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Ukew.Utils
{
    public struct Bits
    {
        // Everything big-endian

        public static Bits Empty => new Bits(ImmutableArray<byte>.Empty);

        private Bits(ImmutableArray<byte> bytes) => _bytes = bytes;

        private readonly ImmutableArray<byte> _bytes;

        public int Count => _bytes.Length;

        public Bits Add(int v) => new Bits(_bytes.AddRange(new byte[]
            {
                (byte)(v >> 24),
                (byte)((v >> 16) & 0xff),
                (byte)((v >> 8) & 0xff),
                (byte)(v & 0xff),
            }));

        public static int GetInt(IReadOnlyList<byte> bytes, int offset) =>
            ((int)bytes[offset + 0] << 24) |
            ((int)bytes[offset + 1] << 16) |
            ((int)bytes[offset + 2] << 8) |
            (int)bytes[offset + 3];

        public Bits Add(int? v) => new Bits(_bytes.Add(v.HasValue ? (byte)1 : (byte)0)).Add(v ?? 0);

        public static int? GetIntN(IReadOnlyList<byte> bytes, int offset) =>
            bytes[offset] != 0 ? GetInt(bytes, offset + 1) : (int?)null;

        public Bits Add(uint v) => new Bits(_bytes.AddRange(new byte[]
            {
                (byte)(v >> 24),
                (byte)((v >> 16) & 0xff),
                (byte)((v >> 8) & 0xff),
                (byte)(v & 0xff),
            }));

        public static uint GetUInt(IReadOnlyList<byte> bytes, int offset) =>
            ((uint)bytes[offset + 0] << 24) |
            ((uint)bytes[offset + 1] << 16) |
            ((uint)bytes[offset + 2] << 8) |
            (uint)bytes[offset + 3];

        public Bits Add(ushort v) => new Bits(_bytes.AddRange(new byte[]
            {
                (byte)(v >> 8),
                (byte)(v & 0xff),
            }));

        public static ushort GetUShort(IReadOnlyList<byte> bytes, int offset) => (ushort)(
            ((ushort)bytes[offset + 0] << 8) |
            (ushort)bytes[offset + 1]);

        public Bits AddFletcher16 => new Bits(_bytes.AddRange(FletcherChecksum.Calc16Bytes(_bytes)));

        public Bits Concat(ImmutableArray<byte> bytes) => new Bits(_bytes.AddRange(bytes));

        public static implicit operator ImmutableArray<byte> (Bits bits) => bits._bytes;
    }
}

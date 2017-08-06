using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;

namespace Ukew.Utils
{
    public struct Bits : IReadOnlyList<byte>
    {
        // Everything big-endian

        public static Bits Empty => new Bits(ImmutableArray<byte>.Empty);

        private Bits(ImmutableArray<byte> bytes) => _bytes = bytes;

        private readonly ImmutableArray<byte> _bytes;

        public int Count => _bytes.Length;

        public Bits AddInt(int v) => new Bits(_bytes.AddRange(new byte[]
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

        public Bits AddIntN(int? v) => new Bits(_bytes.Add(v.HasValue ? (byte)1 : (byte)0)).AddInt(v ?? 0);

        public static int? GetIntN(IReadOnlyList<byte> bytes, int offset) =>
            bytes[offset] != 0 ? GetInt(bytes, offset + 1) : (int?)null;

        public Bits AddUInt(uint v) => new Bits(_bytes.AddRange(new byte[]
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

        public Bits AddUShort(ushort v) => new Bits(_bytes.AddRange(new byte[]
            {
                (byte)(v >> 8),
                (byte)(v & 0xff),
            }));

        public static ushort GetUShort(IReadOnlyList<byte> bytes, int offset) => (ushort)(
            ((ushort)bytes[offset + 0] << 8) |
            (ushort)bytes[offset + 1]);

        public Bits AddShort(short v) => new Bits(_bytes.AddRange(new byte[]
            {
                (byte)(v >> 8),
                (byte)(v & 0xff),
            }));

        public static short GetShort(IReadOnlyList<byte> bytes, int offset) => (short)(
            ((ushort)bytes[offset + 0] << 8) |
            (ushort)bytes[offset + 1]);

        public Bits AddFletcher16 => new Bits(_bytes.AddRange(FletcherChecksum.Calc16Bytes(_bytes)));

        public Bits Concat(ImmutableArray<byte> bytes) => new Bits(_bytes.AddRange(bytes));

        public byte this[int index] => _bytes[index];

        public IEnumerator<byte> GetEnumerator() => _bytes.Skip(0).GetEnumerator();
        IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();

        public static implicit operator ImmutableArray<byte>(Bits bits) => bits._bytes;
    }
}

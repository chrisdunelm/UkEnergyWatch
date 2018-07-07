using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew.Storage
{
    public class Strings
    {
        public const int PartLengthV1 = 64;
        public const byte TerminationByte = 0xff; // 0xff is never valid in UTF8

        public class Map<T> where T : IStringMap<T>
        {
            public Map(T data, params string[] strings)
            {
                Data = data;
                Strings = strings;
            }

            public T Data { get; }
            public IReadOnlyList<string> Strings { get; }

            public async Task<T> WriteStringsAsync(ITaskHelper taskHelper, Strings strings)
            {
                var indexes = new List<long>();
                foreach (var s in Strings)
                {
                    indexes.Add(await strings.AddOrGetIndexAsync(s).ConfigureAwait(taskHelper));
                }
                return Data.CloneWithStrings(indexes);
            }
        }

        public class Reader : DataStoreReader<Data, Data>
        {
            public Reader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "strings") { }
        }

        public class Writer : DataStoreWriter<Data, Data>
        {
            public Writer(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir, "strings") { }
        }

        public struct Data : IStorable<Data, Data>, IStorableFactory<Data>, IEquatable<Data>
        {
            private Data(ImmutableArray<byte> bytes, bool validate)
            {
                if (validate && bytes.Length != PartLengthV1)
                {
                    throw new ArgumentException($"array length must be {PartLengthV1}; found {bytes.Length}", nameof(bytes));
                }
                _bytes = bytes;
            }
            private Data(ReadOnlySpan<byte> bytes, bool validate)
            {
                if (validate && bytes.Length != PartLengthV1)
                {
                    throw new ArgumentException($"array length must be {PartLengthV1}; found {bytes.Length}", nameof(bytes));
                }
                _bytes = bytes.ToArray().ToImmutableArray();
            }

            public Data(ImmutableArray<byte> bytes) : this(bytes, true) { }

            private readonly ImmutableArray<byte> _bytes;

            public ImmutableArray<byte> Bytes => _bytes;

            int IStorableFactory<Data>.CurrentVersion => 1;

            ImmutableArray<byte> IStorableFactory<Data>.Store(Data item) => item._bytes;

            Data IStorableFactory<Data>.Load(int version, ReadOnlySpan<byte> bytes)
            {
                switch (version)
                {
                    case 1:
                        return new Data(Bits.GetBytes(bytes, 0, PartLengthV1), false);
                    default:
                        throw new Exception($"Unknown version: {version}");
                }
            }

            public static bool operator ==(Data a, Data b) => a._bytes.SequenceEqual(b._bytes);
            public override int GetHashCode()
            {
                // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
                ulong hash = 0xcbf29ce484222325UL;
                for (int i = 0; i < _bytes.Length; i += 1)
                {
                    hash *= 1099511628211UL;
                    hash ^= _bytes[i];
                }
                return (int)(hash ^ (hash >> 32));
            }

            public override bool Equals(object obj) => (obj is Data other) && this == other;
            public bool Equals(Data other) => this == other;
            public static bool operator !=(Data a, Data b) => !(a == b);

            public override string ToString()
            {
                var sb = new StringBuilder();
                var bs = Bytes;
                for (int i = 0; i < bs.Length; i += 1)
                {
                    var b = bs[i];
                    sb.Append(b >= 32 && b < 0x80 ? (char)b : '?');
                }
                return sb.ToString();
            }
        }

        public Strings(ITaskHelper taskHelper, IDirectory dir)
        {
            _taskHelper = taskHelper;
            _reader = new Reader(taskHelper, dir);
            _writer = new Writer(taskHelper, dir);
        }

        private readonly ITaskHelper _taskHelper;
        private readonly Reader _reader;
        private readonly Writer _writer;

        private readonly object _lock = new object();

        public async Task<IAsyncEnumerable<(long index, string value)>> AllStrings()
        {
            var raw = await _reader.ReadAsync().ConfigureAwait(_taskHelper);
            var fullIndex = -1L;
            var full = ImmutableArray<byte>.Empty;
            return raw
                .Select((item, index) =>
                {
                    var part = item.Bytes.TakeWhile(b => b != TerminationByte).ToImmutableArray();
                    full = full.AddRange(part);
                    if (fullIndex == -1)
                    {
                        fullIndex = index;
                    }
                    if (part.Length < item.Bytes.Length)
                    {
                        var ret = full;
                        var retIndex = fullIndex;
                        full = ImmutableArray<byte>.Empty;
                        fullIndex = -1;
                        return (retIndex, Encoding.UTF8.GetString(ret.ToArray()));
                    }
                    return (0, null);
                })
                .Where(x => x.Item2 != null);
        }

        public async Task<string> GetAsync(long index)
        {
            var raw = await _reader.ReadAsync((int)index).ConfigureAwait(_taskHelper);
            var full = ImmutableArray<byte>.Empty;
            await raw.TakeWhile(item =>
            {
                var part = item.Bytes.TakeWhile(b => b != TerminationByte).ToImmutableArray();
                full = full.AddRange(part);
                return !(part.Length < item.Bytes.Length);
            }).ToArray();
            return Encoding.UTF8.GetString(full.ToArray());
        }

        private Task<long> AddInternalAsync(string value)
        {
            byte[] bytes = Encoding.UTF8.GetBytes(value);
            List<Data> data = new List<Data>();
            const int partLength = PartLengthV1;
            bool done = false;
            for (int i = 0; !done ; i += partLength)
            {
                var length = Math.Min(partLength, bytes.Length - i);
                var partBytes = ImmutableArray.Create(bytes, i, length);
                if (length < partLength)
                {
                    partBytes = partBytes.Add(TerminationByte);
                    partBytes = partBytes.AddRange(Enumerable.Repeat((byte)0, partLength - partBytes.Length));
                    done = true;
                }
                data.Add(new Data(partBytes));
            }
            return _writer.AppendAsync(data);
        }

        // Not thread-safe
        public Task<long> AddAsync(string value)
        {
            return AddInternalAsync(value);
        }

        // Not thread-safe
        public async Task<long> AddOrGetIndexAsync(string value)
        {
            // TODO: This is horribly inefficient!
            var items = await AllStrings().ConfigureAwait(_taskHelper);
            var (index, str) = await items.FirstOrDefault(s => s.value == value).ConfigureAwait(_taskHelper);
            if (str != null) {
                return index;
            }
            return await AddInternalAsync(value).ConfigureAwait(_taskHelper);
        }
    }

    public static class StringsExtensions
    {
        public static async Task<IReadOnlyList<T>> WriteStringsAsync<T>(this IReadOnlyList<Strings.Map<T>> datas,
            ITaskHelper taskHelper, Strings strings) where T : IStringMap<T>
        {
            var result = new List<T>();
            foreach (var data in datas)
            {
                result.Add(await data.WriteStringsAsync(taskHelper, strings).ConfigureAwait(taskHelper));
            }
            return result;
        }
    }
}

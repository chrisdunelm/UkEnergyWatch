using System;
using System.Threading;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;

namespace Ukew.MemDb
{
    public abstract class DbReader<T> : IChunkedEnumerable<T> where T : struct
    {

        protected DbReader(int requestedBlockSize)
        {
            if (requestedBlockSize == 0)
            {
                int tSize = System.Runtime.InteropServices.Marshal.SizeOf<T>();
                requestedBlockSize = 2_000_000 / tSize; // Make each block take ~2MB
            }
            // Minimum block size is 2^8 = 256 items
            for (_blockPower = 8; ; _blockPower += 1)
            {
                _blockSize = 1 << _blockPower;
                if (_blockSize >= requestedBlockSize)
                {
                    break;
                }
            }
            _blockMask = _blockSize - 1;
        }

        protected readonly int _blockPower;
        protected readonly int _blockSize;
        protected readonly int _blockMask;
        private List<T[]> _blocks = new List<T[]>();
        private T[] _lastData;
        private int _count = 0;

        public long Count => Interlocked.Add(ref _count, 0);

        protected void Add(T item)
        {
            var index = _count & _blockMask;
            if (index == 0)
            {
                _lastData = new T[_blockSize];
                lock (_blocks)
                {
                    _blocks.Add(_lastData);
                }
            }
            // Use Interlocked to ensure that the item is fully written
            // before count increment, and to ensure a read of _count is correct.
            _lastData[index] = item;
            Interlocked.Increment(ref _count);
        }

        // IReadOnlyList<(int blockSize, T[] block)> IDbReader<T>.GetBlocks()
        // {
        //     // Take a copy of everything, to snapshot the current state.
        //     // Writes may be happening concurrently.
        //     int count = Interlocked.Add(ref _count, 0);
        //     var result = new List<(int blockSize, T[] block)>(_blocks.Count);
        //     foreach(var block in _blocks)
        //     {
        //         var blockSize = Math.Min(count, block.Length);
        //         result.Add((blockSize, block));
        //         count -= blockSize;
        //     }
        //     return result;
        // }

        //IEnumerator IEnumerable.GetEnumerator() => ((IEnumerable<Chunk<T>>)this).GetEnumerator();

        IEnumerator<Chunk<T>> IChunkedEnumerable<T>.GetEnumerator()
        {
            // Not lazy for efficiency, and for simplicity of concurrency.
            // Take a snapshot of count, as writes can occur concurrently.
            int count = Interlocked.Add(ref _count, 0);
            T[][] blocks;
            lock (_blocks)
            {
                blocks = _blocks.ToArray();
            }
            // Care needed, as there may be more blocks, or more items in the last block.
            var numBlocks = count == 0 ? 0 : ((count - 1) >> _blockPower) + 1;
            var result = new Chunk<T>[numBlocks];
            for (int i = 0; i < numBlocks; i++)
            {
                var block = blocks[i];
                var blockLength = Math.Min(count, block.Length);
                result[i] = new Chunk<T>(blockLength, block);
                count -= blockLength;
            }
            IEnumerable<A> AsEnum<A>(A[] a) => a;
            return AsEnum(result).GetEnumerator();
        }

        public class Reversed : IChunkedEnumerable<T>
        {
            internal Reversed(DbReader<T> source) => _source = source;
            private DbReader<T> _source;
            IEnumerator<Chunk<T>> IChunkedEnumerable<T>.GetEnumerator()
            {
                // Take a snapshot of count, as writes can occur concurrently.
                int count = Interlocked.Add(ref _source._count, 0);
                T[][] blocks;
                lock (_source._blocks)
                {
                    blocks = _source._blocks.ToArray();
                }
                // Care needed, as there may be more blocks, or more items in the last block.
                var numBlocks = count == 0 ? 0 : ((count - 1) >> _source._blockPower) + 1;
                int blockSize = ((count - 1) & _source._blockMask) + 1;
                if (count != 0 && blockSize == 0) blockSize = _source._blockSize;
                for (int i = numBlocks - 1; i >= 0; i--)
                {
                    var block = blocks[i];
                    var result = new T[blockSize];
                    int blockSizeM1 = blockSize - 1;
                    for (int j = 0; j < result.Length; j++)
                    {
                        result[j] = block[blockSizeM1 - j];
                    }
                    yield return new Chunk<T>(blockSize, result);
                    blockSize = _source._blockSize;
                }
            }
        }

        public Reversed ReverseView => new Reversed(this);
    }

    public static class DbReaderExtensions
    {
        public static IEnumerable<T> AsEnumerable<T>(this IChunkedEnumerable<T> source) where T : struct
        {
            var en = source.GetEnumerator();
            while (en.MoveNext())
            {
                var len = en.Current.DataLength;
                var data = en.Current.Data;
                for (int i = 0; i < len; i++)
                {
                    yield return data[i];
                }
            }
        }

        public static ImmutableArray<T> ToImmutableArray<T>(this IChunkedEnumerable<T> source) where T : struct
        {
            var b = ImmutableArray.CreateBuilder<T>();
            var en = source.GetEnumerator();
            while (en.MoveNext())
            {
                var chunk = en.Current;
                b.AddRange(chunk.Data, chunk.DataLength);
            }
            return b.ToImmutable();
        }

        public static T? FirstOrDefault<T>(this IChunkedEnumerable<T> source) where T : struct
        {
            var en = source.GetEnumerator();
            while (en.MoveNext())
            {
                if (en.Current.DataLength > 0)
                {
                    return en.Current.Data[0];
                }
            }
            return null;
        }

        private struct ChunkedEnumerable<T> : IChunkedEnumerable<T> where T : struct
        {
            public ChunkedEnumerable(IEnumerable<Chunk<T>> en) => _en = en;
            private IEnumerable<Chunk<T>> _en;
            public IEnumerator<Chunk<T>> GetEnumerator() => _en.GetEnumerator(); 
        }

        public static IChunkedEnumerable<TResult> Select<T, TResult>(this IChunkedEnumerable<T> source,
            Func<T, TResult> selector) where T : struct where TResult : struct
        {
            IEnumerable<Chunk<TResult>> Inner(IChunkedEnumerable<T> src, Func<T, TResult> sel)
            {
                var en = src.GetEnumerator();
                while (en.MoveNext())
                {
                    var cur = en.Current;
                    var result = new TResult[cur.DataLength];
                    for (int i = 0; i < result.Length; i++)
                    {
                        result[i] = sel(cur.Data[i]);
                    }
                    yield return new Chunk<TResult>(result.Length, result);
                }
            }
            return new ChunkedEnumerable<TResult>(Inner(source, selector));
        }

        public static IChunkedEnumerable<T> Where<T>(this IChunkedEnumerable<T> source,
            Func<T, bool> predicate) where T : struct
        {
            IEnumerable<Chunk<T>> Inner(IChunkedEnumerable<T> src, Func<T, bool> pred)
            {
                var en = src.GetEnumerator();
                Chunk<T> cur = default;
                T[] writeBlock = null;
                int writeDoneOfs = -1, writeOfs = 0, readOfs = 0;
                while (true)
                {
                    if (readOfs == cur.DataLength)
                    {
                        if (!en.MoveNext()) break;
                        cur = en.Current;
                        readOfs = 0;
                    }
                    if (writeOfs > writeDoneOfs)
                    {
                        if (writeOfs > 0) yield return new Chunk<T>(writeOfs, writeBlock);
                        writeBlock = new T[Math.Max(100, cur.DataLength)];
                        writeDoneOfs = writeBlock.Length * 3 / 4;
                        writeOfs = 0;
                    }
                    int maxReadOfs = Math.Min(readOfs + (writeBlock.Length - writeOfs), cur.DataLength);
                    for (; readOfs < maxReadOfs; readOfs++)
                    {
                        var item = cur.Data[readOfs];
                        if (pred(item)) writeBlock[writeOfs++] = item;
                    }
                }
                if (writeOfs > 0)
                {
                    yield return new Chunk<T>(writeOfs, writeBlock);
                }
            }
            return new ChunkedEnumerable<T>(Inner(source, predicate));
        }

        public static IChunkedEnumerable<TResult> WhereSelect<T, TResult>(this IChunkedEnumerable<T> source,
            Func<T, TResult?> predicateSelect) where T : struct where TResult : struct
        {
            IEnumerable<Chunk<TResult>> Inner(IChunkedEnumerable<T> src, Func<T, TResult?> predSel)
            {
                var en = src.GetEnumerator();
                Chunk<T> cur = default;
                TResult[] writeBlock = null;
                int writeDoneOfs = -1, writeOfs = 0, readOfs = 0;
                while (true)
                {
                    if (readOfs == cur.DataLength)
                    {
                        if (!en.MoveNext()) break;
                        cur = en.Current;
                        readOfs = 0;
                    }
                    if (writeOfs > writeDoneOfs)
                    {
                        if (writeBlock != null) yield return new Chunk<TResult>(writeOfs, writeBlock);
                        writeBlock = new TResult[Math.Max(100, cur.DataLength)];
                        writeDoneOfs = writeBlock.Length * 3 / 4;
                        writeOfs = 0;
                    }
                    int maxReadOfs = Math.Min(readOfs + (writeBlock.Length - writeOfs), cur.DataLength);
                    for (; readOfs < maxReadOfs; readOfs++)
                    {
                        if (predSel(cur.Data[readOfs]) is TResult result) writeBlock[writeOfs++] = result;
                    }
                }
                if (writeOfs > 0)
                {
                    yield return new Chunk<TResult>(writeOfs, writeBlock);
                }
            }
            return new ChunkedEnumerable<TResult>(Inner(source, predicateSelect));
        }

        public static IChunkedEnumerable<T> Take<T>(this IChunkedEnumerable<T> source, int count) where T : struct
        {
            IEnumerable<Chunk<T>> Inner(IChunkedEnumerable<T> src, int c)
            {
                var en = src.GetEnumerator();
                while (c > 0 && en.MoveNext())
                {
                    var cur = en.Current;
                    int size = Math.Min(cur.DataLength, c);
                    yield return new Chunk<T>(size, cur.Data);
                    c -= size;
                }
            }
            return new ChunkedEnumerable<T>(Inner(source, count));
        }

        //public static IEnumerable<(TKey, TAgg)> AggregateBy<T, TKey, TAgg>(IChunkedEnumerable<T> source,
        //    Func<T, TKey> keySelector, Func<IEnumerable)

        // private class DbReaderWriter<T> : DbReader<T> where T : struct
        // {
        //     public DbReaderWriter(int blockSize) : base(blockSize) { }

        //     public new void Add(T item) => base.Add(item);
        // }

        /*public static DbReader<T> Where<T>(this IDbReader<T> source, Func<T, bool> predicate) where T : struct
        {
            var blocks = source.GetBlocks();
            var result = new DbReaderWriter<T>(blocks.Count == 0 ? 1 : blocks[0].blockSize);
            foreach (var (blockSize, block) in blocks)
            {
                for (int i = 0; i < blockSize; i += 1)
                {
                    if (predicate(block[i]))
                    {
                        result.Add(block[i]);
                    }
                }
            }
            return result;
        }

        public static DbReader<TResult> WhereSelect<T, TResult>(this IDbReader<T> source, Func<T, TResult?> predicateProjection)
            where T : struct where TResult : struct
        {
            var blocks = source.GetBlocks();
            var result = new DbReaderWriter<TResult>(blocks.Count == 0 ? 1 : blocks[0].blockSize);
            foreach (var (blockSize, block) in blocks)
            {
                for (int i = 0; i < blockSize; i += 1)
                {
                    if (predicateProjection(block[i]) is TResult item)
                    {
                        result.Add(item);
                    }
                }
            }
            return result;
        }

        public static IReadOnlyDictionary<TKey, DbReader<T>> GroupBy<T, TKey>(this IDbReader<T> source, Func<T, TKey> keyProjection)
            where T : struct
        {
            var result = new Dictionary<TKey, DbReaderWriter<T>>();
            var blocks = source.GetBlocks();
            var segmentBlockSize = Math.Min(10_000, blocks.Count == 0 ? 1 : blocks[0].blockSize);
            foreach (var (blockSize, block) in blocks)
            {
                for (int i = 0; i < blockSize; i += 1)
                {
                    var item = block[i];
                    var key = keyProjection(item);
                    if (!result.TryGetValue(key, out var dbReader))
                    {
                        dbReader = new DbReaderWriter<T>(segmentBlockSize);
                        result.Add(key, dbReader);
                    }
                    dbReader.Add(item);
                }
            }
            return result.ToDictionary(x => x.Key, x => (DbReader<T>)x.Value);
        }

        public static IReadOnlyDictionary<TKey, TResult> GroupBy<T, TKey, TResult>(this IDbReader<T> source,
            Func<T, TKey> keyProjection, Func<DbReader<T>, TResult> resultAggregator)
            where T : struct =>
                source.GroupBy(keyProjection).ToDictionary(x => x.Key, x => resultAggregator(x.Value));

        public static ImmutableArray<T> ToImmutableArray<T>(this IDbReader<T> source) where T : struct
        {
            var blocks = source.GetBlocks();
            var size = blocks.Sum(x => x.blockSize);
            var builder = ImmutableArray.CreateBuilder<T>(size);
            foreach (var (blockSize, block) in blocks)
            {
                builder.AddRange(block.Length == blockSize ? block : block.Take(blockSize));
            }
            return builder.MoveToImmutable();
        }

        public static T Last<T>(this IDbReader<T> source) where T : struct
        {
            var blocks = source.GetBlocks();
            var lastBlock = blocks[blocks.Count - 1];
            return lastBlock.block[lastBlock.blockSize - 1];
        }

        public static T? LastOrDefault<T>(this IDbReader<T> source) where T : struct
        {
            var blocks = source.GetBlocks();
            if (blocks.Count == 0)
            {
                return null;
            }
            var lastBlock = blocks[blocks.Count - 1];
            if (lastBlock.blockSize == 0)
            {
                return null;
            }
            return lastBlock.block[lastBlock.blockSize - 1];
        }*/
    }
}

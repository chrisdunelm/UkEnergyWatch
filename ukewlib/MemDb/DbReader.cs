using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;

namespace Ukew.MemDb
{
    public abstract class DbReader<T> : IDbReader<T> where T : struct
    {

        protected DbReader(int requestedBlockSize)
        {
            if (requestedBlockSize == 0)
            {
                int tSize = System.Runtime.InteropServices.Marshal.SizeOf<T>();
                requestedBlockSize = 2_000_000 / tSize; // Make each block take ~2MB
            }
            // Minimum block size is 2^6 = 64 items
            for (_blockPower = 6; ; _blockPower += 1)
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

        protected int Count => _count;

        protected void Add(T item)
        {
            var index = _count & _blockMask;
            if (index == 0)
            {
                _lastData = new T[_blockSize];
                _blocks.Add(_lastData);
            }
            _lastData[index] = item;
            _count += 1;
        }

        IReadOnlyList<(int blockSize, T[] block)> IDbReader<T>.GetBlocks()
        {
            int count = _count;
            var result = new List<(int blockSize, T[] block)>(_blocks.Count);
            foreach(var block in _blocks)
            {
                var blockSize = Math.Min(count, block.Length);
                result.Add((blockSize, block));
                count -= blockSize;
            }
            return result;
        }
    }

    public static class DbReaderExtensions
    {
        private class DbReaderWriter<T> : DbReader<T> where T : struct
        {
            public DbReaderWriter(int blockSize) : base(blockSize) { }

            public new void Add(T item) => base.Add(item);
        }

        public static DbReader<T> Where<T>(this IDbReader<T> source, Func<T, bool> predicate) where T : struct
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
    }
}

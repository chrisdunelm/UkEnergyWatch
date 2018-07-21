namespace Ukew.MemDb
{
    public readonly struct Chunk<T>
    {
        public Chunk(int dataLength, T[] data)
        {
            DataLength = dataLength;
            Data = data;
        }
        public int DataLength { get; }
        public T[] Data { get; }
    }
}

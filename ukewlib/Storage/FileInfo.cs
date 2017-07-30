namespace Ukew.Storage
{
    public class FileInfo
    {
        public FileInfo(string id, long length) => (Id, Length) = (id, length);
        public string Id { get; }
        public long Length { get; }
    }
}

using System;
using System.Globalization;

namespace Ukew.Storage
{
    public abstract class DataStore
    {
        internal const byte ID_BYTE_1 = 0xfe;
        internal const byte ID_BYTE_2 = 0x01;

        protected class FileName
        {
            // "<prefix>.seqid.<int:8x>.version.<int:x>.elementsize.<int:x>.datastore"

            public FileName(FileInfo fileInfo)
            {
                var parts = fileInfo.Id.Split('.');
                if (parts.Length != 8 ||
                    parts[1] != "seqid" ||
                    parts[3] != "version" ||
                    parts[5] != "elementsize" ||
                    parts[7] != "datastore")
                {
                    throw new ArgumentException($"Invalid name: '{fileInfo.Id}'");
                }
                Id = fileInfo.Id;
                Length = fileInfo.Length;
                Prefix = parts[0];
                SeqId = int.Parse(parts[2], NumberStyles.HexNumber);
                Version = int.Parse(parts[4], NumberStyles.HexNumber);
                ElementSize = int.Parse(parts[6], NumberStyles.HexNumber);
            }

            public FileName(string prefix, int seqId, int version, int elementSize)
            {
                Id = $"{prefix}.seqid.{seqId:x8}.version.{version:x}.elementsize.{elementSize:x}.datastore";
                Length = 0;
                Prefix = prefix;
                SeqId = seqId;
                Version = version;
                ElementSize = elementSize;
            }

            public string Id { get; }
            public long Length { get; }
            public string Prefix { get; }
            public int SeqId { get; }
            public int Version { get; }
            public int ElementSize { get; }

            public long ElementCount => Length / ElementSize;

            public override string ToString() =>
                $"{{ Id:'{Id}', Length:{Length}, Prefix:{Prefix} SeqId:{SeqId}, Version:{Version}, ElementSize:{ElementSize} }}";
        }
    }
}

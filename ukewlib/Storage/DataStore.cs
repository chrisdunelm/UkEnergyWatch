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
            // "seqid.<int:8x>.version.<int:x>.elementsize.<int:x>.datastore"

            public FileName(FileInfo fileInfo)
            {
                var parts = fileInfo.Id.Split('.');
                if (parts.Length != 7 ||
                    parts[0] != "seqid" ||
                    parts[2] != "version" ||
                    parts[4] != "elementsize" ||
                    parts[6] != "datastore")
                {
                    throw new ArgumentException($"Invalid name: '{fileInfo.Id}'");
                }
                Id = fileInfo.Id;
                Length = fileInfo.Length;
                SeqId = int.Parse(parts[1], NumberStyles.HexNumber);
                Version = int.Parse(parts[3], NumberStyles.HexNumber);
                ElementSize = int.Parse(parts[5], NumberStyles.HexNumber);
            }

            public FileName(int seqId, int version, int elementSize)
            {
                Id = $"seqid.{seqId:x8}.version.{version:x}.elementsize.{elementSize:x}.datastore";
                Length = 0;
                SeqId = seqId;
                Version = version;
                ElementSize = elementSize;
            }

            public string Id { get; }
            public long Length { get; }
            public int SeqId { get; }
            public int Version { get; }
            public int ElementSize { get; }

            public override string ToString() => $"{{ Id:'{Id}', Length:{Length}, SeqId:{SeqId}, Version:{Version}, ElementSize:{ElementSize} }}";
        }
    }
}

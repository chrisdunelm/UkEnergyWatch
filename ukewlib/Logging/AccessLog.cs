using System;
using System.Net;
using NodaTime;
using Ukew.Storage;
using Ukew.Utils.Tasks;
/*
namespace Ukew.Logging
{
    public class AccessLog
    {
        public class Reader : DataStoreReader<Data, Data>
        {
            public Reader(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir) { }
        }

        public class Writer : DataStoreWriter<Data, Data>
        {
            public Writer(ITaskHelper taskHelper, IDirectory dir) : base (taskHelper, dir) { }
        }

        public struct Data : IStorable<Data, Data>, IStorableFactory<Data>, IEquatable<Data>
        {
            private Data(uint requestIpAddress, uint requestUnixSeconds, ushort requestTimeMillis, ushort requestUrlIndex,
                ushort requestReferrerUrlIndex, ushort requestUserAgentIndex, ushort responseDelayMs, ushort _responseCode)
            {
                throw new Exception();
            }

            private readonly uint _requestIpAddress; // Big endian: 0x2414188f == 143.24.20.36
            private readonly uint _requestUnixSeconds;
            private readonly ushort _requestTimeMillis;
            private readonly ushort _requestUrlIndex;
            private readonly ushort _requestReferrerUrlIndex;
            private readonly ushort _requestUserAgentIndex;
            private readonly ushort _responseDelayMs;
            private readonly ushort _responseCode;

            public IPAddress IpAddress => new IPAddress(_requestIpAddress);
            public Instant RequestTime => Instant.FromUnixTimeSeconds(_requestUnixSeconds) + Duration.FromMilliseconds(_requestTimeMillis);
            public int ReuqestUrlIndex => _requestUrlIndex;
            public int RequestReferrerUrlIndex => _requestReferrerUrlIndex;
            public int RequestUserAgentIndex => _requestUserAgentIndex;
            public Duration ResponseDelay => Duration.FromMilliseconds(_responseDelayMs);
            public int ResponseCode => _responseCode;
        }
    }
}
*/

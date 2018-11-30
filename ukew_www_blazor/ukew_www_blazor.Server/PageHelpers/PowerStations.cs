using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using NodaTime;
using Ukew.Elexon;
using Ukew.MemDb;
using Ukew.Utils;
using UnitsNet;

namespace ukew_www_blazor.Server.PageHelpers
{
    public class PowerStations
    {
        private readonly static object s_seenLock = new object();
        private readonly static NodaTime.Duration s_seenUpdateInterval = NodaTime.Duration.FromHours(24);
        private static Instant s_nextSeenUpdate = Instant.MinValue;
        private static HashSet<string> s_seen = null;

        public static HashSet<string> Seen(ITime time, Db<B1610.Data> b1610Db)
        {
            lock (s_seenLock)
            {
                var now = time.GetCurrentInstant();
                if (now > s_nextSeenUpdate)
                {
                    s_nextSeenUpdate = now + s_seenUpdateInterval;
                    s_seen = new HashSet<string>(
                        b1610Db.ReverseView.Take(20_000).AsEnumerable().Select(x => x.ResourceName)
                    );
                }
                return s_seen;
            }
        }

        private static readonly IReadOnlyDictionary<EicIds.FuelType, int> s_fuelTypeOrder = new Dictionary<EicIds.FuelType, int>()
        {
            { EicIds.FuelType.Ccgt, 1 },
            { EicIds.FuelType.Ocgt, 2 },
            { EicIds.FuelType.Oil, 3 },
            { EicIds.FuelType.Coal, 4 },
            { EicIds.FuelType.Nuclear, 5 },
            { EicIds.FuelType.WindOnshore, 6 },
            { EicIds.FuelType.WindOffshore, 7 },
            { EicIds.FuelType.Wind, 8 },
            { EicIds.FuelType.Solar, 9 },
            { EicIds.FuelType.PumpedStorage, 10 },
            { EicIds.FuelType.Hydro, 11 },
            { EicIds.FuelType.Other, 12 },
            { EicIds.FuelType.Unknown, 13 },
        };

        public static IReadOnlyList<(EicIds.FuelType fuelType, IReadOnlyList<EicIds.PowerStation> powerStations)> ByFuelType()
        {
            return EicIds.PowerStations
                .Where(x => x.GenerationUnitRegisteredResourceNames.Any())
                .Where(x => !EicIds.InactiveResourceNames.Contains(x.RegisteredResourceName))
                .GroupBy(x => x.FuelType)
                .OrderBy(x => s_fuelTypeOrder[x.Key])
                .Select(x => (x.Key, (IReadOnlyList<EicIds.PowerStation>)x.OrderBy(y => y.RegisteredResourceName).ToList()))
                .ToList();
        }
    }
}

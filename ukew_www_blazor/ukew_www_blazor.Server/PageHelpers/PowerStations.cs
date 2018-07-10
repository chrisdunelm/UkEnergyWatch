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

        /*public static async Task<PowerStations> New(ITime time, Db<B1610.Data> b1610Db, Db<PhyBmData.FpnData> fpnDb)
        {
            Instant now = time.GetCurrentInstant();
            await fpnDb.InitialiseTask;
            var fpnData = fpnDb.ReverseView.Take(2_000).ToImmutableArray().Reverse().ToList();
            await b1610Db.InitialiseTask;
            var b1610Data = b1610Db.ReverseView.Take(1_000).ToImmutableArray().Reverse().ToList();
            var genUnitFpn = new Dictionary<string, (Power power, Instant update)>();
            foreach (var fpn in fpnData.Where(x => now >= x.TimeFrom))
            {
                Power? level = fpn.LevelAt(now);
                genUnitFpn[fpn.ResourceName] = (level ?? fpn.LevelTo, level != null ? now : fpn.TimeTo);
            }
            var genUnitB1610 = new Dictionary<string, (Power power, Instant update)>();
            foreach (var b1610 in b1610Data)
            {
                genUnitB1610[b1610.ResourceName] = (b1610.Power, b1610.SettlementPeriodStart + NodaTime.Duration.FromMinutes(30));
            }
            var lastInstant = b1610Data.Select(x => x.SettlementPeriodStart).Max();
            var b1610ResourceNamesSeen = new HashSet<string>(b1610Db.ReverseView.Take(10_000).AsEnumerable().Select(x => x.ResourceName));
            foreach (var b1610Name in b1610ResourceNamesSeen)
            {
                if (!genUnitB1610.ContainsKey(b1610Name))
                {
                    genUnitB1610.Add(b1610Name, (Power.Zero, lastInstant));
                }
            }
            var powerStations = EicIds.PowerStations
                .Where(x => x.GenerationUnitRegisteredResourceNames.Any())
                .Where(x => !EicIds.InactiveResourceNames.Contains(x.RegisteredResourceName));

            var fuelTypeOrder = new Dictionary<EicIds.FuelType, int>
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

            var ret = EicIds.PowerStations
                .Where(x => x.GenerationUnitRegisteredResourceNames.Any())
                .Where(x => !EicIds.InactiveResourceNames.Contains(x.RegisteredResourceName))
                .Select((powerStation, psIndex) =>
                {
                    var co2KgPerJoule = co2KgPerJoules.TryGetValue(powerStation.FuelType, out var v) ? v : 0.0;
                    var psGenUnits = powerStation.GenerationUnitRegisteredResourceNames
                        .Select(resourceName =>
                        {
                            PowerStationsModel.GenModel.Generation MakeGen(Dictionary<string, (Power power, Instant update)> data)
                            {
                                var haveData = data.TryGetValue(resourceName, out var data1);
                                return new PowerStationsModel.GenModel.Generation
                                {
                                    Power = haveData ? data1.power : (Power?)null,
                                    UpdateTime = haveData ? data1.update.InZone(s_tzLondon) : (ZonedDateTime?)null,
                                    Co2 = haveData ? data1.power.CalculateCo2(co2KgPerJoule) : (MassFlow?)null,
                                };
                            }
                            return new PowerStationsModel.GenModel.GeneratingUnit
                            {
                                UnderlyingGenerationUnit = EicIds.GenerationUnitsByResourceName[resourceName],
                                FpnGeneration = MakeGen(genUnitFpn),
                                B1610Generation = MakeGen(genUnitB1610),
                            };
                        })
                        .ToList();
                    PowerStationsModel.GenModel.Generation MakePsGen(Func<PowerStationsModel.GenModel.GeneratingUnit, PowerStationsModel.GenModel.Generation> fn, bool needsAll)
                    {
                        var genUnitData = psGenUnits.Select(fn).ToList();
                        var allGenUnitsHaveData = needsAll ? genUnitData.All(x => x.Power != null) : genUnitData.Any(x => x.Power != null);
                        return new PowerStationsModel.GenModel.Generation
                        {
                            Power = allGenUnitsHaveData ? genUnitData.Aggregate(Power.Zero, (total, x) => total + (x.Power ?? Power.Zero)) : (Power?)null,
                            UpdateTime = allGenUnitsHaveData ? genUnitData.FirstOrDefault(x => x.UpdateTime.HasValue)?.UpdateTime : (ZonedDateTime?)null,
                            Co2 = allGenUnitsHaveData ? genUnitData.Aggregate(MassFlow.Zero, (total, x) => total + (x.Co2 ?? MassFlow.Zero)) : (MassFlow?)null,
                        };
                    }
                    return new PowerStationsModel.GenModel.PowerStation
                    {
                        UnderlyingPowerStation = powerStation,
                        FpnGeneration = MakePsGen(x => x.FpnGeneration, true),
                        B1610Generation = MakePsGen(x => x.B1610Generation, false),
                        GeneratingUnits = psGenUnits,
                        CssId = $"__ps_{psIndex}",
                    };
                })
                .OrderBy(x => x.UnderlyingPowerStation.RegisteredResourceName)
                .GroupBy(x => x.UnderlyingPowerStation.FuelType)
                .OrderBy(x => fuelTypeOrder[x.Key])
                .ToList();
            return new PowerStationsModel.GenModel(ret);
        }*/
    }
}

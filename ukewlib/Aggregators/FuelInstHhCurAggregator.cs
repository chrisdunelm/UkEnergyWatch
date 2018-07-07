// using System;
// using System.Collections.Generic;
// using System.Linq;
// using System.Threading.Tasks;
// using NodaTime;
// using Ukew.Elexon;
// using Ukew.MemDb;
// using Ukew.Storage;
// using Ukew.Utils.Tasks;
// using UnitsNet;

// namespace Ukew.Aggregators
// {
//     public class FuelInstHhCurAggregator
//     {
//         private static readonly DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

//         public static async Task<FuelInstHhCurAggregator> CreateAsync(ITaskHelper taskHelper, IDirectory dir)
//         {
//             var reader = new FuelInstHhCur.Reader(taskHelper, dir);
//             var memDb = new Db<FuelInstHhCur.Data>(taskHelper, reader);
//             await memDb.InitialiseTask.ConfigureAwait(taskHelper);
//             return new FuelInstHhCurAggregator(memDb);
//         }

//         private FuelInstHhCurAggregator(Db<FuelInstHhCur.Data> db) => _db = db;

//         private Db<FuelInstHhCur.Data> _db;

//         public IList<(LocalDate date, int count, FuelInstHhCur.Data data)> ByDay(LocalDate fromInclusive, LocalDate toExclusive)
//         {
//             var fromInstant = fromInclusive.AtStartOfDayInZone(s_tzLondon).ToInstant();
//             var toInstant = toExclusive.AtStartOfDayInZone(s_tzLondon).ToInstant();
//             return Aggregated(data =>
//             {
//                 var update = data.Update;
//                 if (update >= fromInstant && update < toInstant)
//                 {
//                     var date = update.InZone(s_tzLondon).Date;
//                     return (date, data);
//                 }
//                 return ((LocalDate date, FuelInstHhCur.Data data)?)null;
//             });
//         }

//         internal static LocalDate FirstDateOfWeek(LocalDate date, IsoDayOfWeek weekStartDay)
//         {
//             var sub = ((int)date.DayOfWeek - (int)weekStartDay + 7) % 7;
//             return date.PlusDays(-sub);
//         }

//         public IList<(LocalDate date, int count, FuelInstHhCur.Data data)> ByWeek(
//             LocalDate fromInclusive, LocalDate toExclusive, IsoDayOfWeek weekStartDay = IsoDayOfWeek.Sunday)
//         {
//             var fromInstant = fromInclusive.AtStartOfDayInZone(s_tzLondon).ToInstant();
//             var toInstant = toExclusive.AtStartOfDayInZone(s_tzLondon).ToInstant();
//             return Aggregated(data =>
//             {
//                 var update = data.Update;
//                 if (update >= fromInstant && update < toInstant)
//                 {
//                     var date = FirstDateOfWeek(update.InZone(s_tzLondon).Date, weekStartDay);
//                     return (date, data);
//                 }
//                 return ((LocalDate date, FuelInstHhCur.Data data)?)null;
//             });
//         }

//         public IList<(LocalDate month, int count, FuelInstHhCur.Data data)> ByMonth(LocalDate fromInclusive, LocalDate toExclusive)
//         {
//             var fromInstant = fromInclusive.AtStartOfDayInZone(s_tzLondon).ToInstant();
//             var toInstant = toExclusive.AtStartOfDayInZone(s_tzLondon).ToInstant();
//             return Aggregated(data =>
//             {
//                 var update = data.Update;
//                 if (update >= fromInstant && update < toInstant)
//                 {
//                     var date = update.InZone(s_tzLondon).Date;
//                     date = new LocalDate(date.Year, date.Month, 1);
//                     return (date, data);
//                 }
//                 return ((LocalDate date, FuelInstHhCur.Data data)?)null;
//             });
//         }

//         public IList<(TKey key, int count, FuelInstHhCur.Data data)> Aggregated<TKey>(
//             Func<FuelInstHhCur.Data, (TKey key, FuelInstHhCur.Data data)?> fn)
//         {
//             var byKey = _db.WhereSelect(fn);
//             var grouped = byKey.GroupBy(x => x.key, xs =>
//             {
//                 var a = xs.ToImmutableArray();
//                 var ccgt = Power.Zero;
//                 var ocgt = Power.Zero;
//                 var oil = Power.Zero;
//                 var coal = Power.Zero;
//                 var nuclear = Power.Zero;
//                 var wind = Power.Zero;
//                 var ps = Power.Zero;
//                 var npshyd = Power.Zero;
//                 var other = Power.Zero;
//                 var intFr = Power.Zero;
//                 var intIrl = Power.Zero;
//                 var intNed = Power.Zero;
//                 var intEw = Power.Zero;
//                 for (int i = 0; i < a.Length; i += 1)
//                 {
//                     var data = a[i].data;
//                     ccgt += data.Ccgt;
//                     ocgt += data.Ocgt;
//                     oil += data.Oil;
//                     coal += data.Coal;
//                     nuclear += data.Nuclear;
//                     wind += data.Wind;
//                     ps += data.Ps;
//                     npshyd += data.Npshyd;
//                     other += data.Other;
//                     intFr += data.IntFr;
//                     intIrl += data.IntIrl;
//                     intNed += data.IntNed;
//                     intEw += data.IntEw;
//                 }
//                 var result = new FuelInstHhCur.Data(Instant.MinValue,
//                     ccgt, ocgt, oil, coal, nuclear, wind, ps, npshyd, other, intFr, intIrl, intNed, intEw);
//                 return (length: a.Length, result);
//             });
//             return grouped.OrderBy(x => x.Key).Select(x => (x.Key, x.Value.length, x.Value.result)).ToList();
//         }
//     }
// }

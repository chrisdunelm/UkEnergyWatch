using System;
using System.Collections.Generic;
using NodaTime;
using Ukew.Elexon;
using Ukew.MemDb;
using ukew_www_blazor.Shared;

namespace ukew_www_blazor.Server.Data
{
    public class DataSource
    {
        private interface ISource<A> where A : struct
        {
            DbReader<A> Get(Instant from, Instant to);
        }

        private interface IExtract<A, B> where A : struct where B : struct
        {
            DbReader<B> Extract(DbReader<A> data);
        }

        private class ElexonFuelInst : ISource<FuelInstHhCur.Data>
        {
            public ElexonFuelInst(Db<FuelInstHhCur.Data> db)
            {
                _db = db;
            }

            private Db<FuelInstHhCur.Data> _db;

            public DbReader<FuelInstHhCur.Data> Get(Instant from, Instant to)
            {
                //return _db.Where(x => x.Update >= from && x.Update < to);
                throw new NotImplementedException();
            }
        }

        //public static IReadOnlyDictionary<TimeSeries, ()> x;
    }
}

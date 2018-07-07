using System;
using NodaTime;

namespace Ukew.Utils
{
    public static class PrimitiveExtensions
    {
        public static int InRange(this int i, int min, int max) => i < min ? min : i > max ? max : i;

        public static void Locked<TLock>(this TLock o, Action fn) where TLock : class
        {
            lock (o)
            {
                fn();
            }
        }

        public static T Locked<TLock, T>(this TLock o, Func<T> fn) where TLock : class
        {
            lock (o)
            {
                return fn();
            }
        }

        private static DateTimeZone s_tzLondon = DateTimeZoneProviders.Tzdb["Europe/London"];

        public static LocalDate SettlementDate(this Instant instant) => instant.InZone(s_tzLondon).Date;

        public static int SettlementPeriod(this Instant instant)
        {
            var duration = instant - s_tzLondon.AtStartOfDay(instant.SettlementDate()).ToInstant();
            return (int)duration.TotalMinutes / 30 + 1;
        }

        public static Instant SettlementPeriodStart(this (LocalDate, int) dateAndPeriod)
        {
            var (date, period) = dateAndPeriod;
            return date.AtStartOfDayInZone(s_tzLondon).ToInstant() + Duration.FromMinutes(30) * (period - 1);
        }

        public static string Append(this string s, string t) => s + t;
    }
}

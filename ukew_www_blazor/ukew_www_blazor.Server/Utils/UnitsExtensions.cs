using UnitsNet;

namespace ukew_www_blazor.Server.Utils
{
    public static class UnitsExtensions
    {
        public static string DisplayMW(this Power power) => power.Megawatts.ToString("N0") + " MW";

        public static string DisplayKg_s(this MassFlow flow) => flow.KilogramsPerSecond.ToString("N0") + " kg/s";

        public static string DisplayKg_kWh(this double co2KgPerKWh) => co2KgPerKWh.ToString("N3") + " kg/kWh";

        public static string DisplayHz(this Frequency frequency) => frequency.Hertz.ToString("N3") + " Hz";

        public static string DisplayM3_s(this Flow flow) => flow.CubicMetersPerSecond.ToString("N1") + " m³/s";

        public static string DisplayMm3_day(this Flow flow) => (flow.CubicMetersPerHour * 24 / 1e6).ToString("N2") + " Mm³/day";
    }
}

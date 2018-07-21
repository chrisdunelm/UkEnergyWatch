using UnitsNet;

namespace Ukew.Utils
{
    public static class UnitsCo2Extensions
    {
        // TODO: Calculate these properly!
        private static MassFlow CalculateCo2(this Power power, double wToKgPerS) => MassFlow.FromKilogramsPerSecond(power.Watts * wToKgPerS);

        public static MassFlow CcgtCo2(this Power power) => power.CalculateCo2(1.02006335797254e-7);
        public static MassFlow OcgtCo2(this Power power) => MassFlow.Zero;
        public static MassFlow OilCo2(this Power power) => MassFlow.Zero;
        public static MassFlow CoalCo2(this Power power) => power.CalculateCo2(2.71604938271605e-7);
    }
}

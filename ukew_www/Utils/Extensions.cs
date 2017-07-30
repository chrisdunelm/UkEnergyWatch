using UnitsNet;

namespace Ukew.Utils
{
    public static class Extensions
    {
        public static MassFlow CalculateCo2(this Power power, double wToKgPerS) => MassFlow.FromKilogramsPerSecond(power.Watts * wToKgPerS);
    }
}

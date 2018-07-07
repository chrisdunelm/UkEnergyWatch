using UnitsNet;

namespace Ukew.Utils
{
    public static class UnitsExtensions
    {
        public static MassFlow CcgtCo2(this Power power) => MassFlow.Zero;
        public static MassFlow OcgtCo2(this Power power) => MassFlow.Zero;
        public static MassFlow OilCo2(this Power power) => MassFlow.Zero;
        public static MassFlow CoalCo2(this Power power) => MassFlow.Zero;
    }
}

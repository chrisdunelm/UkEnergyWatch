using Ukew.Elexon;

namespace ukew_www_blazor.Server.Utils
{
    public static class PowerStationExtensions
    {
        public static string CssId(this EicIds.PowerStation ps) => ps.RegisteredResourceName;
    }
}

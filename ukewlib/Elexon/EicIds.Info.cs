using System.Collections.Generic;
using Ukew.Utils;

namespace Ukew.Elexon
{
    public static partial class EicIds
    {
        public static IReadOnlyList<PowerStationInfo> PowerStationInfos { get; } = new PowerStationInfo[]
        {
            // OCGT
            new PowerStationInfo("BAGE", new Location(51.616113, -3.833532), "http://calonenergy.com/power-stations/baglan-bay/", "https://en.wikipedia.org/wiki/Baglan_Bay_power_station", "https://upload.wikimedia.org/wikipedia/commons/3/35/Baglan_bay_power_station.jpg"),
            new PowerStationInfo("BRGG", new Location(53.541167, -0.505782), null, "https://en.wikipedia.org/wiki/Glanford_Brigg_Power_Station", "https://upload.wikimedia.org/wikipedia/commons/5/53/Brigg_Power_Station_-_geograph.org.uk_-_1580995.jpg"),
            // Coal
            new PowerStationInfo("DRAXX", new Location(53.735833, -0.996389),  "https://www.drax.com/", "https://en.wikipedia.org/wiki/Drax_power_station", "https://upload.wikimedia.org/wikipedia/commons/3/3e/Northeast_of_Drax_-_geograph.org.uk_-_581958.jpg"),
            // Wind
            new PowerStationInfo("ARCHW", new Location(55.053333, -4.882222), "https://www.scottishpowerrenewables.com/pages/arecleoch_windfarm.aspx", "https://en.wikipedia.org/wiki/Arecleoch_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/8/8b/Geograph-3419211-Arecleoch-Wind-Farm.jpg"),
            new PowerStationInfo("BABAW", new Location(58.569646, -3.686060), "http://www.bailliewindfarm.co.uk/", null, "https://www.statkraft.com/link/b0fd5cfd48a647f09684a797c4acd691.aspx"),
            new PowerStationInfo("BLLA", new Location(55.766944, -3.738889), "https://www.scottishpowerrenewables.com/pages/black_law.aspx", "https://en.wikipedia.org/wiki/Black_Law_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/c/ca/Black_Law_Wind_Farm_-_geograph.org.uk_-_166862.jpg"),
            new PowerStationInfo("BOWLW", new Location(53.983333, -3.283333), null, "https://en.wikipedia.org/wiki/Barrow_Offshore_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/a/a7/Barrow_Offshore_wind_turbines_NR.jpg"),
            new PowerStationInfo("BURBW", new Location(53.483333, -3.166667), null, "https://en.wikipedia.org/wiki/Burbo_Bank_Offshore_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/8/83/Pretty_flamingos_-_geograph.org.uk_-_578705.jpg"),
            new PowerStationInfo("GNFSW", new Location(51.739444, 1.174444), null, "https://en.wikipedia.org/wiki/Gunfleet_Sands_Offshore_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/6/6a/Gunfleet_Sands_Offshore_Wind_Farm_-_geograph.org.uk_-_2091181.jpg"),
            new PowerStationInfo("LARYW", new Location(51.626, 1.495), "http://www.londonarray.com/", "https://en.wikipedia.org/wiki/London_Array", "https://upload.wikimedia.org/wikipedia/commons/0/0a/London_Array_02.jpg"),
            new PowerStationInfo("LNCSW", new Location(53.183333, 0.483333), null, "https://en.wikipedia.org/wiki/Lincs_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/3/31/Lincs_Offshore_Wind_Farm_-_geograph.org.uk_-_3802895.jpg"),
            new PowerStationInfo("WDNSO", new Location(53.983, -3.463), null, "https://en.wikipedia.org/wiki/West_of_Duddon_Sands_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/e/eb/West_of_Duddon_Sands_Wind_Farm_2014.jpg"),
            new PowerStationInfo("WHILW", new Location(55.687222, -4.228611), "https://www.scottishpowerrenewables.com/pages/whitelee.aspx", "https://en.wikipedia.org/wiki/Whitelee_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/2/23/Whitelee_with_arran_in_the_background.jpg"),
            new PowerStationInfo("WLNYW", new Location(54.044, -3.522), null, "https://en.wikipedia.org/wiki/Walney_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/e/eb/Walney_Offshore_Windfarm_-_geograph.org.uk_-_2391702.jpg"),
            new PowerStationInfo("WTMSO", new Location(53.805, 0.149), null, "https://en.wikipedia.org/wiki/Westermost_Rough_Wind_Farm", "https://upload.wikimedia.org/wikipedia/commons/9/99/Westermost_Rough_Windfarm_from_Killingholme.jpg"),
            // Pumped Storage
            new PowerStationInfo("CRUA", new Location(56.406389, -5.113056), "https://www.scottishpower.com/pages/cruachan_power_station.aspx", "https://en.wikipedia.org/wiki/Cruachan_Power_Station", "https://upload.wikimedia.org/wikipedia/commons/c/cd/Dam_at_Cruachan_reservoir.jpg"),
            new PowerStationInfo("DINO", new Location(53.118611, -4.113889), "http://www.fhc.co.uk/dinorwig.htm", "https://en.wikipedia.org/wiki/Dinorwig_Power_Station", "https://upload.wikimedia.org/wikipedia/commons/0/05/DinorwigPowerStation01.jpg"),
            new PowerStationInfo("FFES", new Location(52.980833, -3.968889), "http://www.fhc.co.uk/ffestiniog.htm", "https://en.wikipedia.org/wiki/Ffestiniog_Power_Station", "https://upload.wikimedia.org/wikipedia/commons/d/de/Stwlan.dam.jpg"),
            new PowerStationInfo("FOYE", new Location(57.255338, -4.493343), "http://sse.com/whatwedo/ourprojectsandassets/renewables/foyers/", null, "https://upload.wikimedia.org/wikipedia/commons/c/cf/Foyers_Power_Station%2C_Loch_Ness_-_geograph.org.uk_-_621434.jpg"),
        };
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using UnitsNet;

namespace Ukew.Elexon
{
    public static partial class EicIds
    {
        public enum FuelType
        {
            Unknown,
            Coal,
            Ocgt,
            Ccgt,
            Oil,
            Wind,
            WindOffshore,
            WindOnshore,
            Solar,
            Nuclear,
            Hydro,
            PumpedStorage,
            Other,
        }

        public class PowerStation
        {
            public PowerStation(
                string registeredResourceName,
                string registeredResourceEic,
                string assetName,
                string responsibleParty,
                FuelType fuelType,
                Power? installedCapacity,
                ElectricPotential? voltageLimit,
                IEnumerable<string> generationUnitRegisteredResourceNames)
            {
                RegisteredResourceName = registeredResourceName;
                RegisteredResourceEic = registeredResourceEic;
                AssetName = assetName;
                ResponsibleParty = responsibleParty;
                FuelType = fuelType;
                InstalledCapacity = installedCapacity;
                VoltageLimit = voltageLimit;
                GenerationUnitRegisteredResourceNames = generationUnitRegisteredResourceNames;
            }

            public string RegisteredResourceName { get; } // "Display Name" in EIC xls
            public string RegisteredResourceEic { get; } // "Energy Identification Code" in EIC xls
            public string AssetName { get; }
            public string ResponsibleParty { get; }
            public FuelType FuelType { get; }
            public Power? InstalledCapacity { get; } // Not in EIC xls
            public ElectricPotential? VoltageLimit { get; } // Not in EIC xms
            public IEnumerable<string> GenerationUnitRegisteredResourceNames { get; } // Derived from EIC xls
        }

        public class GenerationUnit
        {
            public GenerationUnit(
                string registeredResourceName,
                string powerStationRegisteredResourceName,
                string registeredResourceEic,
                string assetName,
                string responsibleParty,
                FuelType fuelType,
                Power? maxCapacity,
                string bmUnitId)
            {
                RegisteredResourceName = registeredResourceName;
                PowerStationRegisteredResourceName = powerStationRegisteredResourceName;
                RegisteredResourceEic = registeredResourceEic;
                AssetName = assetName;
                ResponsibleParty = responsibleParty;
                FuelType = fuelType;
                MaxCapacity = maxCapacity;
                BmUnitId = bmUnitId;
                RegisteredResourceNameHash = Hash(registeredResourceName);
            }

            public string RegisteredResourceName { get; } // "Display Name" in EIC xls
            public string PowerStationRegisteredResourceName { get; } // "Station Name" in EIC xls
            public string RegisteredResourceEic { get; } // "Energy Identification Code" in EIC xls
            public string AssetName { get; }
            public string ResponsibleParty { get; }
            public FuelType FuelType { get; }
            public Power? MaxCapacity { get; }
            public string BmUnitId { get; } // Not in EIC xls

            public uint RegisteredResourceNameHash { get; }
        }

        public static uint Hash(string s)
        {
            // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
            ulong hash = 0xcbf29ce484222325UL;
            for (int i = 0; i < s.Length; i += 1)
            {
                hash *= 1099511628211UL;
                hash ^= s[i];
            }
            return (uint)(hash ^ (hash >> 32));
        }

        static EicIds()
        {
            // Needs to be in a static ctor, to force correct initialized order.
            GenerationUnitsByResourceNameHash = GenerationUnits.ToDictionary(x => x.RegisteredResourceNameHash);
            GenerationUnitsByResourceName = GenerationUnits.ToDictionary(x => x.RegisteredResourceName);
        }

        public static Dictionary<uint, GenerationUnit> GenerationUnitsByResourceNameHash { get; }

        public static Dictionary<string, GenerationUnit> GenerationUnitsByResourceName { get; }

        public static GenerationUnit LookupResourceNameHash(uint hash) =>
            GenerationUnitsByResourceNameHash.TryGetValue(hash, out var ret) ? ret : null;

        public static HashSet<string> InactiveResourceNames = new HashSet<string>
        {
            // List of all active: https://www.gov.uk/government/uploads/system/uploads/attachment_data/file/633779/Chapter_5.pdf
            // "Power stations in the UK. Operational at the end of May 2017"
            // Not sure how complete this is for smaller BMUnits - e.g. wind

            // Nuclear
            "OLDS", "WYLF",
            // Ccgt
            // Wind
            // Coal
            "LOAN", "FERR", "RUGPS",
            // Ocgt
            "FERR-G", "LITT-G",
            // Oil
            "LITT-D", "GRAI",
            // Biomass
            // Pumped Storage
        };
    }
}
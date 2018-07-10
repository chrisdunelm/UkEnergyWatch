using System;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using NodaTime;
using Ukew.Elexon;
using Ukew.MemDb;
using Ukew.NationalGrid;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace ukew_www.Controllers
{
    public class LegacyAppApiController : Controller
    {
        public LegacyAppApiController(ITaskHelper taskHelper,
            Db<FuelInstHhCur.Data> fuelInstDb,
            Db<Freq.Data> freqDb,
            InstantaneousFlow.Reader gasFlowReader,
            Db<InstantaneousFlow.Data> gasFlowDb)
        {
            _taskHelper = taskHelper;
            _fuelInstDb = fuelInstDb;
            _freqDb = freqDb;
            _gasFlowReader = gasFlowReader;
            _gasFlowDb = gasFlowDb;
        }

        private readonly ITaskHelper _taskHelper;
        private readonly Db<FuelInstHhCur.Data> _fuelInstDb;
        private readonly Db<Freq.Data> _freqDb;
        private readonly InstantaneousFlow.Reader _gasFlowReader;
        private readonly Db<InstantaneousFlow.Data> _gasFlowDb;

        [HttpPost("/Auth/Login.asmx")]
        public IActionResult AuthLogin()
        {
            var soapAction = Request.Headers["SOAPAction"][0].Trim('"', ' ');
            switch (soapAction)
            {
                case "http://data.ukenergywatch.org.uk/Auth1":
                    var token = Guid.NewGuid().ToString();
                    var challenge = Convert.ToBase64String(new byte[] { 0 });
                    string response1 = $@"
<?xml version=""1.0"" encoding=""utf-16""?>
<soap:Envelope xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
    xmlns:xsd=""http://www.w3.org/2001/XMLSchema""
    xmlns:soap=""http://schemas.xmlsoap.org/soap/envelope/"">
<soap:Body>
    <Auth1Response xmlns=""http://data.ukenergywatch.org.uk/"">
    <Auth1Result>
        <Token>{token}</Token>
        <Challenge>{challenge}</Challenge>
    </Auth1Result>
    </Auth1Response>
</soap:Body>
</soap:Envelope>".Trim();
                    return Content(response1, "text/xml");
                case "http://data.ukenergywatch.org.uk/Auth2":
                    string response2 = @"
<?xml version=""1.0"" encoding=""utf-16""?>
<soap:Envelope xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
      xmlns:xsd=""http://www.w3.org/2001/XMLSchema""
      xmlns:soap=""http://schemas.xmlsoap.org/soap/envelope/"">
  <soap:Body>
    <Auth2Response xmlns=""http://data.ukenergywatch.org.uk/"">
      <Auth2Result>true</Auth2Result>
    </Auth2Response>
  </soap:Body>
</soap:Envelope>".Trim();
                    return Content(response2, "text/xml");
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }

        [HttpPost("/Data/Summary.asmx")]
        public async Task<IActionResult> DataSummary()
        {
            var soapAction = Request.Headers["SOAPAction"][0].Trim('"', ' ');
            switch (soapAction)
            {
                case "http://data.ukenergywatch.org.uk/GetSummary":
                    await _fuelInstDb.InitialiseTask;
                    var fuelInst = _fuelInstDb.ReverseView.FirstOrDefault().Value;
                    await _freqDb.InitialiseTask;
                    var freq = _freqDb.ReverseView.FirstOrDefault().Value;
                    await _gasFlowDb.InitialiseTask;
                    var gasLastTotal = _gasFlowDb.ReverseView.Take(1000).AsEnumerable()
                        .Where(x => x.Type == InstantaneousFlow.SupplyType.TotalSupply)
                        .OrderByDescending(x => x.Update)
                        .FirstOrDefault();

                    string response = $@"
<?xml version=""1.0"" encoding=""utf-16""?>
<soap:Envelope xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
      xmlns:xsd=""http://www.w3.org/2001/XMLSchema""
      xmlns:soap=""http://schemas.xmlsoap.org/soap/envelope/"">
  <soap:Body>
    <GetSummaryResponse xmlns=""http://data.ukenergywatch.org.uk/"">
      <GetSummaryResult>
        <ElecTotalGenMw>
          <When>{fuelInst.Update}</When>
          <Value>{(int)(fuelInst.Total.Megawatts)}</Value>
        </ElecTotalGenMw>
        <ElecFrequency>
          <When>{freq.Update}</When>
          <Value>{freq.Frequency.Hertz}</Value>
        </ElecFrequency>
        <GasTotalFlowIn>
          <When>{gasLastTotal.Update}</When>
          <Value>{gasLastTotal.FlowRate.CubicMetersPerHour * 24 / 1e6}</Value>
        </GasTotalFlowIn>
      </GetSummaryResult>
    </GetSummaryResponse>
  </soap:Body>
</soap:Envelope>".Trim();
                    return Content(response, "text/xml");
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }

        [HttpPost("/Data/Electricity.asmx")]
        public async Task<IActionResult> DataElectricity()
        {
            var soapAction = Request.Headers["SOAPAction"][0].Trim('"', ' ');
            switch (soapAction)
            {
                case "http://data.ukenergywatch.org.uk/GetCurrentDetails":
                    await _fuelInstDb.InitialiseTask;
                    var fuelInst = _fuelInstDb.ReverseView.FirstOrDefault().Value;

                    string response = $@"
<?xml version=""1.0"" encoding=""utf-16""?>
<soap:Envelope xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
      xmlns:xsd=""http://www.w3.org/2001/XMLSchema""
      xmlns:soap=""http://schemas.xmlsoap.org/soap/envelope/"">
  <soap:Body>
    <GetCurrentDetailsResponse xmlns=""http://data.ukenergywatch.org.uk/"">
      <GetCurrentDetailsResult>
        <When>{fuelInst.Update}</When>
        <SettlementDate>{fuelInst.Update.SettlementDate().AtMidnight().InUtc().ToInstant()}</SettlementDate>
        <SettlementPeriod>{fuelInst.Update.SettlementPeriod()}</SettlementPeriod>
        <Ccgt>{(int)(fuelInst.Ccgt.Megawatts)}</Ccgt>
        <Ocgt>{(int)(fuelInst.Ocgt.Megawatts)}</Ocgt>
        <Oil>{(int)(fuelInst.Oil.Megawatts)}</Oil>
        <Coal>{(int)(fuelInst.Coal.Megawatts)}</Coal>
        <Nuclear>{(int)(fuelInst.Nuclear.Megawatts)}</Nuclear>
        <Wind>{(int)(fuelInst.Wind.Megawatts)}</Wind>
        <Ps>{(int)(fuelInst.Ps.Megawatts)}</Ps>
        <NPsHyd>{(int)(fuelInst.Npshyd.Megawatts)}</NPsHyd>
        <IntFr>{(int)(fuelInst.IntFr.Megawatts)}</IntFr>
        <IntIrl>{(int)(fuelInst.IntIrl.Megawatts + fuelInst.IntEw.Megawatts)}</IntIrl>
        <IntNed>{(int)(fuelInst.IntNed.Megawatts)}</IntNed>
        <Other>{(int)(fuelInst.Other.Megawatts)}</Other>
      </GetCurrentDetailsResult>
    </GetCurrentDetailsResponse>
  </soap:Body>
</soap:Envelope>".Trim();
                    return Content(response, "text/xml");
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }

        [HttpPost("/Data/Gas.asmx")]
        public async Task<IActionResult> DataGas()
        {
            var soapAction = Request.Headers["SOAPAction"][0].Trim('"', ' ');
            switch (soapAction)
            {
                case "http://data.ukenergywatch.org.uk/GetCurrentFlows":
                    await _gasFlowDb.InitialiseTask;
                    var lastFew = _gasFlowDb.ReverseView.Take(1000).ToImmutableArray();
                    var lastTotal = lastFew
                        .Where(x => x.Type == InstantaneousFlow.SupplyType.TotalSupply)
                        .OrderByDescending(x => x.Update)
                        .FirstOrDefault();
                    
                    var strings = _gasFlowReader.Strings;
                    async Task<string> Details(InstantaneousFlow.SupplyType supplyType) =>
                        (await lastFew
                            .Where(x => x.Type == supplyType && x.Update == lastTotal.Update)
                            .SelectAsync(_taskHelper, async flow => (flow, name: await flow.NameAsync(strings))))
                            .Select(x => $"<Detail><Location>{x.name}</Location><FlowRate>{x.flow.FlowRate.CubicMetersPerHour * 24 / 1e6}</FlowRate></Detail>")
                            .Aggregate(new StringBuilder(256), (a, x) => a.Append(x), x => x.ToString());

                    string response = $@"
<?xml version=""1.0"" encoding=""utf-16""?>
<soap:Envelope xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
      xmlns:xsd=""http://www.w3.org/2001/XMLSchema""
      xmlns:soap=""http://schemas.xmlsoap.org/soap/envelope/"">
  <soap:Body>
    <GetCurrentFlowsResponse xmlns=""http://data.ukenergywatch.org.uk/"">
      <GetCurrentFlowsResult>
        <When>{lastTotal.Update}</When>
        <ZoneSupply>{await Details(InstantaneousFlow.SupplyType.ZoneSupply)}</ZoneSupply>
        <TerminalSupply>{await Details(InstantaneousFlow.SupplyType.TerminalSupply)}</TerminalSupply>
        <TotalSupply>{await Details(InstantaneousFlow.SupplyType.TotalSupply)}</TotalSupply>
      </GetCurrentFlowsResult>
    </GetCurrentFlowsResponse>
  </soap:Body>
</soap:Envelope>".Trim();
                    return Content(response, "text/xml");
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }
    }
}

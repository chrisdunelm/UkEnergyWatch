using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using NodaTime;
using Ukew.Elexon;
using Ukew.Utils;

namespace ukew_www.Controllers
{
    public class DataController : Controller
    {
        public DataController(FuelInstHhCur.Reader fuelInstHhCurReader, Freq.Reader freqReader)
        {
            _fuelInstHhCurReader = fuelInstHhCurReader;
            _freqReader = freqReader;
        }

        private readonly FuelInstHhCur.Reader _fuelInstHhCurReader;
        private readonly Freq.Reader _freqReader;

        [HttpPost("/Auth/Login.asmx")]
        public IActionResult AuthLogin()
        {
            Response.ContentType = "text/xml; charset=utf-8";
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
                    return Ok(response1);
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
                    return Ok(response2);
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }

        [HttpPost("/Data/Summary.asmx")]
        public async Task<IActionResult> DataSummary()
        {
            Response.ContentType = "text/xml; charset=utf-8";
            var soapAction = Request.Headers["SOAPAction"][0].Trim('"', ' ');
            switch (soapAction)
            {
                case "http://data.ukenergywatch.org.uk/GetSummary":
                    var fuelInstCount = (int)await _fuelInstHhCurReader.CountAsync();
                    var fuelInstData = await _fuelInstHhCurReader.ReadAsync(fuelInstCount - 1, fuelInstCount);
                    var fuelInst = await fuelInstData.First();
                    var freqCount = (int)await _freqReader.CountAsync();
                    var freqData = await _freqReader.ReadAsync(freqCount - 1, freqCount);
                    var freq = await freqData.First();
                    var zeroInstant = Instant.FromUtc(2000, 1, 1, 0, 0);

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
          <When>{zeroInstant}</When>
          <Value>0.0</Value>
        </GasTotalFlowIn>
      </GetSummaryResult>
    </GetSummaryResponse>
  </soap:Body>
</soap:Envelope>".Trim();
                    return Ok(response);
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }

        [HttpPost("/Data/Electricity.asmx")]
        public async Task<IActionResult> DataElectricity()
        {
            Response.ContentType = "text/xml; charset=utf-8";
            var soapAction = Request.Headers["SOAPAction"][0].Trim('"', ' ');
            switch (soapAction)
            {
                case "http://data.ukenergywatch.org.uk/GetCurrentDetails":
                    var fuelInstCount = (int)await _fuelInstHhCurReader.CountAsync();
                    var fuelInstData = await _fuelInstHhCurReader.ReadAsync(fuelInstCount - 1, fuelInstCount);
                    var fuelInst = await fuelInstData.First();

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
                    return Ok(response);
                default:
                    throw new InvalidOperationException($"Invalid SOAP action: '{soapAction}'");
            }
        }
    }
}

using CommandLine;
using Microsoft.AspNetCore;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace ukew_www_blazor.Server
{
    public class CmdLineOptions
    {
        [Option(Required = true, HelpText = "Absolute or relative directory path for fuel-instance half-hour storage")]
        public string FuelInstHhCurDataDirectory { get; set; }

        [Option(Required = true, HelpText = "Absolute or relative directory path for grid frequency storage")]
        public string FreqDataDirectory { get; set; }

        [Option(Required = true, HelpText = "Absolute or relative directory path for Physical FPN storage")]
        public string FpnDataDirectory { get; set; }

        [Option(Required = true, HelpText = "Absolute or relative directory path for B1610 (actual per-unit generation) storage")]
        public string B1610DataDirectory { get; set; }

        [Option(Required = true, HelpText = "Absolute or relative directory path for gas-flow storage")]
        public string GasFlowDataDirectory { get; set; }

        [Option(Required = false, HelpText = "True to listen on all network interfaces, not just localhost")]
        public bool NonLocalhost { get; set; } = false;

        [Option(Required = false, HelpText = "Absolute or relative directory path for access log. Defaults to ./logs")]
        public string AccessLogDirectory { get; set; } = "./logs";
    }

    public class Program
    {
        public static void Main(string[] args)
        {
            var cmdLineResult = Parser.Default.ParseArguments<CmdLineOptions>(args) as Parsed<CmdLineOptions>;
            if (cmdLineResult == null)
            {
                return;
            }

            BuildWebHost(args, cmdLineResult.Value).Run();
        }

        public static IWebHost BuildWebHost(string[] args, CmdLineOptions options)
        {
            var builder = WebHost.CreateDefaultBuilder(args)
                .UseConfiguration(new ConfigurationBuilder()
                    .AddCommandLine(args)
                    .Build())
                .ConfigureServices(services =>
                {
                    services.AddSingleton(options);
                })
                .UseStartup<Startup>();

            if (options.NonLocalhost)
            {
                builder = builder.UseUrls("http://*:5000");
            }

            return builder.Build();
        }
    }
}

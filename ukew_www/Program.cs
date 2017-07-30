using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using CommandLine;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;

namespace ukew_www
{
    public class CmdLineOptions
    {
        [Option(Required = true, HelpText = "Absolute or relative directory path for fuel-instance half-hour storage")]
        public string FuelInstHhCurDataDirectory { get; set; }

        [Option(Required = false, HelpText = "Runtime environment: 'Development', 'Staging', 'Production'")]
        public string Environment { get; set; } = "Development";

        [Option(Required = false, HelpText = "True to listen on all network interfaces, not just localhost")]
        public bool NonLocalhost { get; set; } = false;
    }

    public class Program
    {
        public static void Main(string[] args)
        {
            var cmdLineOptions = new CmdLineOptions();
            var cmdLineResult = Parser.Default.ParseArguments<CmdLineOptions>(args) as Parsed<CmdLineOptions>;
            if (cmdLineResult == null)
            {
                return;
            }

            var hostBuilder = new WebHostBuilder()
                .UseKestrel()
                .UseContentRoot(Directory.GetCurrentDirectory())
                .ConfigureServices(services =>
                {
                    services.AddSingleton(cmdLineResult.Value);
                    services.AddSingleton<IStartup, Startup>();
                })
                .UseEnvironment(cmdLineResult.Value.Environment);
            if (cmdLineResult.Value.NonLocalhost)
            {
                hostBuilder = hostBuilder.UseUrls("http://*:5000");
            }
            
            var host = hostBuilder.Build();

            host.Run();
        }
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace ukew_www
{
    public class Startup : IStartup
    {
        public Startup(IHostingEnvironment env, ILoggerFactory loggerFactory, CmdLineOptions cmdLineOptions)
        {
            _env = env;
            _loggerFactory = loggerFactory;
            var builder = new ConfigurationBuilder()
                .SetBasePath(env.ContentRootPath)
                .AddJsonFile("appsettings.json", optional: false, reloadOnChange: true)
                .AddJsonFile($"appsettings.{env.EnvironmentName}.json", optional: true)
                .AddEnvironmentVariables();
            Configuration = builder.Build();
        }

        private readonly IHostingEnvironment _env;
        private readonly ILoggerFactory _loggerFactory;

        public IConfigurationRoot Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public IServiceProvider ConfigureServices(IServiceCollection services)
        {
            // Add framework services.
            services.AddMvc();

            // Add custom services.
            services.AddSingleton<ITime>(SystemTime.Instance);
            services.AddSingleton<ITaskHelper>(SystemTaskHelper.Instance);
            services.AddSingleton<FuelInstHhCur.Reader>(ctx =>
            {
                var cmdLineOptions = ctx.GetRequiredService<CmdLineOptions>();
                var taskHelper = ctx.GetRequiredService<ITaskHelper>();
                var dir = new SystemDirectory(taskHelper, cmdLineOptions.FuelInstHhCurDataDirectory);
                return new FuelInstHhCur.Reader(taskHelper, dir);
            });
            services.AddSingleton<Freq.Reader>(ctx =>
            {
                var cmdLineOptions = ctx.GetRequiredService<CmdLineOptions>();
                var taskHelper = ctx.GetRequiredService<ITaskHelper>();
                var dir = new SystemDirectory(taskHelper, cmdLineOptions.FreqDataDirectory);
                return new Freq.Reader(taskHelper, dir);
            });
            services.AddSingleton<PhyBmData.FpnReader>(ctx =>
            {
                var cmdLineOptions = ctx.GetRequiredService<CmdLineOptions>();
                var taskHelper = ctx.GetRequiredService<ITaskHelper>();
                var dir = new SystemDirectory(taskHelper, cmdLineOptions.FpnDataDirectory);
                return new PhyBmData.FpnReader(taskHelper, dir);
            });

            return services.BuildServiceProvider();
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app)
        {
            _loggerFactory.AddConsole(Configuration.GetSection("Logging"));
            _loggerFactory.AddDebug();

            if (_env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
                app.UseBrowserLink();
            }
            else
            {
                app.UseExceptionHandler("/Home/Error");
            }

            app.UseStaticFiles();
            app.UseMvc();
        }
    }
}

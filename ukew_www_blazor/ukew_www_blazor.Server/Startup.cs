using Microsoft.AspNetCore.Blazor.Server;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.ResponseCompression;
using Microsoft.AspNetCore.Rewrite;
using Microsoft.Extensions.DependencyInjection;
using Newtonsoft.Json.Serialization;
using NodaTime;
using System.Linq;
using System.Net.Mime;
using Ukew.Elexon;
using Ukew.MemDb;
using Ukew.NationalGrid;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;
using ukew_www_blazor.Server.Blogging;
using ukew_www_blazor.Server.Controllers;
using ukew_www_blazor.Server.Utils;

namespace ukew_www_blazor.Server
{
    public class Startup
    {
        // This method gets called by the runtime. Use this method to add services to the container.
        // For more information on how to configure your application, visit https://go.microsoft.com/fwlink/?LinkID=398940
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddMvc();

            services.AddResponseCompression(options =>
            {
                options.MimeTypes = ResponseCompressionDefaults.MimeTypes.Concat(new[]
                {
                    MediaTypeNames.Application.Octet,
                    WasmMediaTypeNames.Application.Wasm,
                });
            });

            services.AddSingleton<ITime>(SystemTime.Instance);
            services.AddSingleton<ITaskHelper>(SystemTaskHelper.Instance);

            services.AddSingleton<Db<FuelInstHhCur.Data>>(svcs =>
            {
                var taskHelper = svcs.GetRequiredService<ITaskHelper>();
                var options = svcs.GetRequiredService<CmdLineOptions>();
                var dir = new SystemDirectory(taskHelper, options.FuelInstHhCurDataDirectory);
                var reader = new FuelInstHhCur.Reader(taskHelper, dir);
                return new Db<FuelInstHhCur.Data>(taskHelper, reader);
            });
            services.AddSingleton<Db<Freq.Data>>(svcs =>
            {
                var taskHelper = svcs.GetRequiredService<ITaskHelper>();
                var options = svcs.GetRequiredService<CmdLineOptions>();
                var dir = new SystemDirectory(taskHelper, options.FreqDataDirectory);
                var reader = new Freq.Reader(taskHelper, dir);
                return new Db<Freq.Data>(taskHelper, reader);
            });
            services.AddSingleton<InstantaneousFlow.Reader>(svcs =>
            {
                var taskHelper = svcs.GetRequiredService<ITaskHelper>();
                var options = svcs.GetRequiredService<CmdLineOptions>();
                var dir = new SystemDirectory(taskHelper, options.GasFlowDataDirectory);
                return new InstantaneousFlow.Reader(taskHelper, dir);
            });
            services.AddSingleton<Db<InstantaneousFlow.Data>>(svcs =>
            {
                var taskHelper = svcs.GetRequiredService<ITaskHelper>();
                var reader = svcs.GetRequiredService<InstantaneousFlow.Reader>();
                return new Db<InstantaneousFlow.Data>(taskHelper, reader);
            });
            services.AddSingleton<Db<B1610.Data>>(svcs =>
            {
                var taskHelper = svcs.GetRequiredService<ITaskHelper>();
                var options = svcs.GetRequiredService<CmdLineOptions>();
                var dir = new SystemDirectory(taskHelper, options.B1610DataDirectory);
                var reader = new B1610.Reader(taskHelper, dir);
                return new Db<B1610.Data>(taskHelper, reader);
            });
            services.AddSingleton<Db<PhyBmData.FpnData>>(svcs =>
            {
                var taskHelper = svcs.GetRequiredService<ITaskHelper>();
                var options = svcs.GetRequiredService<CmdLineOptions>();
                var dir = new SystemDirectory(taskHelper, options.FpnDataDirectory);
                var reader = new PhyBmData.FpnReader(taskHelper, dir);
                return new Db<PhyBmData.FpnData>(taskHelper, reader, pollInterval: Duration.FromMinutes(5));
            });

            services.AddSingleton<PostBag>();
        }

        private T Di<T>(IApplicationBuilder app) => (T)app.ApplicationServices.GetRequiredService(typeof(T));

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IHostingEnvironment env)
        {
            app.UseRewriter(new RewriteOptions()
                .AddRedirect("^index(.html?)?$", "/")
            );

            var dir = new SystemDirectory(Di<ITaskHelper>(app), Di<CmdLineOptions>(app).AccessLogDirectory);
            app.UseAccessLog(dir);

            app.UseResponseCompression();

            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseStaticFiles();

            app.UseMvc(routes =>
            {
                routes.MapRoute(name: "default", template: "{controller}/{action}/{id?}");
            });

            //app.UseBlazor<Client.Program>();
        }
    }
}

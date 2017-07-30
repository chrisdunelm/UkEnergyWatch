using System.IO;
using System.Threading.Tasks;
using CommandLine;
using NodaTime;
using Ukew.Elexon;
using Ukew.Storage;
using Ukew.Utils;
using Ukew.Utils.Tasks;

namespace Ukew
{
    public static class Program
    {
        public static int Main(string[] args)
        {
            return Parser.Default.ParseArguments<GetFuelInstHhCur.Options, ShowFuelInstHhCur.Options>(args)
                .MapResult(
                    (GetFuelInstHhCur.Options opts) => Task.Run(() => new GetFuelInstHhCur(opts).Run()).Result,
                    (ShowFuelInstHhCur.Options opts) => Task.Run(() => new ShowFuelInstHhCur(opts).Run()).Result,
                    errs => 1
                );
        }
    }
}

using Ukew.Storage;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.MemDb
{
    public class DbTest_SimpleFuncs
    {
        [Fact]
        public void FirstOrDefault() => TimeRunner.Run(async (time, th) =>
        {
            var reader = new FakeReader<int>(th) { 0, 1, 2, 3 };
            using (var db = new Db<int>(th, reader))
            {
                await db.InitialiseTask.ConfigureAwait(th);
                Assert.Equal(0, db.FirstOrDefault());
                Assert.Null(db.Take(0).FirstOrDefault());
            }
        });
    }
}

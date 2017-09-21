using System.Collections.Generic;
using System.Linq;
using Ukew.Testing;
using Ukew.Utils.Tasks;
using Xunit;

namespace Ukew.Storage
{
    public class StringsTest
    {
        [Fact]
        public void AddShort()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory();
                var strings = new Strings(th, dir);
                var all0 = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Empty(all0);

                var index1 = await strings.AddAsync("a").ConfigureAwait(th);
                Assert.Equal(0, index1);
                var all1 = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Equal(new[] { (0L, "a") }, all1);

                var index2 = await strings.AddAsync("b").ConfigureAwait(th);
                Assert.Equal(1, index2);
                var all2 = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Equal(new[] { (0L, "a"), (1L, "b") }, all2);

                Assert.Equal("a", await strings.GetAsync(index1));
                Assert.Equal("b", await strings.GetAsync(index2));
            });
        }

        [Fact]
        public void AddLong()
        {
            TimeRunner.Run(async (time, th) =>
            {
                string s1 = new string('a', 1000);
                string s2 = new string('b', 2000);

                var dir = new FakeDirectory();
                var strings = new Strings(th, dir);
                var all0 = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Empty(all0);

                var index1 = await strings.AddAsync(s1).ConfigureAwait(th);
                Assert.Equal(0, index1);
                var all1 = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Equal(new[] { (0L, s1) }, all1);

                var index2 = await strings.AddAsync(s2).ConfigureAwait(th);
                long expectedIndex2 = s1.Length / Strings.PartLengthV1 + 1;
                Assert.Equal(expectedIndex2, index2);
                var all2 = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                Assert.Equal(new[] { (0L, s1), (expectedIndex2, s2) }, all2);

                Assert.Equal(s1, await strings.GetAsync(index1));
                Assert.Equal(s2, await strings.GetAsync(index2));
            });
        }

        [Fact]
        public void AddMany()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var ls = new[] { 1, 100, 2, 200, Strings.PartLengthV1, Strings.PartLengthV1 - 1, Strings.PartLengthV1 + 1,
                    10000, 1, 10000, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
                var dir = new FakeDirectory();
                var strings = new Strings(th, dir);

                var expected = new List<(long, string)>();

                foreach (var l in ls)
                {
                    var s = new string('a', l);
                    var index = await strings.AddAsync(s).ConfigureAwait(th);
                    expected.Add((index, s));

                    var all = await (await strings.AllStrings().ConfigureAwait(th)).ToList().ConfigureAwait(th);
                    Assert.Equal(expected, all);
                    Assert.Equal(s, await strings.GetAsync(index));
                }
            });
        }

        [Fact]
        public void AddOrGet()
        {
            TimeRunner.Run(async (time, th) =>
            {
                var dir = new FakeDirectory();
                var strings = new Strings(th, dir);

                var index0 = await strings.AddOrGetIndexAsync("a").ConfigureAwait(th);
                var index1 = await strings.AddOrGetIndexAsync("a").ConfigureAwait(th);
                var index2 = await strings.AddOrGetIndexAsync("b").ConfigureAwait(th);
                var index3 = await strings.AddOrGetIndexAsync("b").ConfigureAwait(th);
                var index4 = await strings.AddOrGetIndexAsync("a").ConfigureAwait(th);
                var index5 = await strings.AddOrGetIndexAsync("b").ConfigureAwait(th);

                Assert.Equal(0, index0);
                Assert.Equal(0, index1);
                Assert.Equal(1, index2);
                Assert.Equal(1, index3);
                Assert.Equal(0, index4);
                Assert.Equal(1, index5);
            });
        }
    }
}

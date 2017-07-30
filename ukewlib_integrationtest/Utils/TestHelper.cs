using System;

namespace Ukew.Utils
{
    public static class TestHelper
    {
        private const string TestElexonApiKeyVariableName = "TEST_ELEXON_APIKEY";

        private static Lazy<string> s_ElexonApiKey = new Lazy<string>(() =>
        {
            var elexonApiKey = Environment.GetEnvironmentVariable(TestElexonApiKeyVariableName);
            if (string.IsNullOrWhiteSpace(elexonApiKey))
            {
                throw new InvalidOperationException($"Environment variable '{TestElexonApiKeyVariableName}' must be set to a real elexon API key.");
            }
            return elexonApiKey;
        });

        public static string ElexonApiKey() => s_ElexonApiKey.Value;
    }
}

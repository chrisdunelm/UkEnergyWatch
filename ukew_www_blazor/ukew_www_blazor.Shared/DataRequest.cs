namespace ukew_www_blazor.Shared
{
    public class DataRequest
    {
        public TimeSeries TimeSeriesId { get; set; }
        public long FromSeconds { get; set; }
        public long ToSeconds { get; set; }
        
    }
}

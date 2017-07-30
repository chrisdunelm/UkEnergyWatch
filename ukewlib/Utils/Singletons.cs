namespace Ukew.Utils
{
    public static class Singletons<T> where T : new()
    {
        public static T Instance { get; } = new T();
    }
}

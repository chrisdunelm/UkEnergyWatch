using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Ukew.Storage
{
    public interface IStorableFactory<T>
    {
        int CurrentVersion { get; }
        ImmutableArray<byte> Store(T item);
        T Load(int version, ReadOnlySpan<byte> bytes);
    }

    public interface IStorable<T, TFactory> where TFactory : IStorableFactory<T>, new()
    {
    }
}

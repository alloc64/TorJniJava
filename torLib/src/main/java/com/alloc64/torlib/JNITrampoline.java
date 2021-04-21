package com.alloc64.torlib;

public class JNITrampoline
{
    public interface Callable<V>
    {
        V call() ;
    }

    public <T> T call(Callable<T> method)
    {
        return method.call();
    }

    public void call(Runnable method)
    {
        method.run();
    }
}

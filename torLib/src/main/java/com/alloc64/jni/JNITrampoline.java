package com.alloc64.jni;

/**
 * Used for proxying native method calls, so disassembly looks more confusing for first time.
 * However, this may get optimized out by R8 anyway.
 */
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

package org.zeroprism;

import java.io.FileDescriptor;

public class Transport
{
    public native String stringFromJNI();

    public native String version();

    public native boolean createTorConfiguration();

    public native void mainConfigurationFree();

    public native static FileDescriptor prepareFileDescriptor(String path);

    public native boolean mainConfigurationSetCommandLine(String[] args);

    public native boolean setupControlSocket();

    public native int runMain();

}

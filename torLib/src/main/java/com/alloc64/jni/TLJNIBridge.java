package com.alloc64.jni;

import android.util.Log;

import com.alloc64.http.ProxiedSocketFactory;
import com.alloc64.torlib.PdnsdConfig;
import com.alloc64.torlib.TorConfig;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.OkHttpClient;

public class TLJNIBridge
{
    public class Tor
    {
        private TorControlSocket controlPortSocket;

        public String getTorVersion()
        {
            return jniTrampoline.call(TLJNIBridge.this::a1);
        }

        public Tor createTorConfig() throws IllegalStateException
        {
            if (!jniTrampoline.call(TLJNIBridge.this::a2))
                throw new IllegalStateException("Unable to create transport config.");

            return this;
        }

        public Tor destroyTorConfig()
        {
            jniTrampoline.call(TLJNIBridge.this::a3);
            return this;
        }

        public Tor setTorCommandLine(String[] args)
        {
            if (!jniTrampoline.call(() -> a5(args)))
                throw new IllegalStateException("Unable to set command line arguments.");

            return this;
        }

        public Tor setTorCommandLine(TorConfig torConfig)
        {
            setTorCommandLine(torConfig.asCommands());
            return this;
        }

        public Tor attachControlPort(InetSocketAddress socketAddress, PasswordDigest password, TorControlSocket.TorEventHandler eventHandler)
        {
            this.controlPortSocket = new TorControlSocket(password, eventHandler);
            controlPortSocket.connect(socketAddress);

            return this;
        }

        public TorControlSocket getControlPortSocket()
        {
            return controlPortSocket;
        }

        public Tor startTor()
        {
            jniTrampoline.call(TLJNIBridge.this::a6);
            return this;
        }

        public OkHttpClient.Builder createOkHttpClient(InetSocketAddress socketAddress)
        {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socketAddress);

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.socketFactory(new ProxiedSocketFactory(proxy));
            builder.proxy(proxy);

            return builder;
        }
    }

    public class Pdnsd
    {
        public Pdnsd startDnsd(String[] args)
        {
            jniTrampoline.call(() -> a8(args));
            return this;
        }

        public Pdnsd startDnsd(PdnsdConfig config)
        {
            return startDnsd(config.asCommands());
        }

        public void destroyDnsd()
        {
            jniTrampoline.call(TLJNIBridge.this::a9);
        }
    }

    public class Tun2Socks
    {
        public void createInterface(
                int vpnInterfaceFileDescriptor,
                int vpnInterfaceMTU,
                String vpnIpAddress,
                String vpnNetMask,
                String socksServerAddress,
                String udpgwServerAddress,
                int udpgwTransparentDNS)
        {
            jniTrampoline.call(() -> TLJNIBridge.this.a10(vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, vpnNetMask, socksServerAddress, udpgwServerAddress, udpgwTransparentDNS));
        }

        public void destroyInterface()
        {
            jniTrampoline.call(TLJNIBridge.this::a11);
        }
    }

    public interface LogProvider
    {
        void logNativeMessage(int priority, String tag, String message);
    }

    private final JNITrampoline jniTrampoline = new JNITrampoline();

    private final Tor tor = new Tor();
    private final Pdnsd pdnsd = new Pdnsd();
    private final Tun2Socks tun2Socks = new Tun2Socks();
    private LogProvider logProvider;

    public TLJNIBridge()
    {
        setLogProvider(Log::println);
    }

    public Tor getTor()
    {
        return tor;
    }

    public Pdnsd getPdnsd()
    {
        return pdnsd;
    }

    public Tun2Socks getTun2Socks()
    {
        return tun2Socks;
    }

    public void setLogProvider(LogProvider logProvider)
    {
        this.logProvider = logProvider;
    }

    // region Tor native methods

    public native String a1();

    public native boolean a2();

    public native void a3();

    public native boolean a5(String[] args);

    public native void a6();

    // endregion

    // region Dnsd native methods

    public native void a8(String[] args);

    public native void a9();

    // endregion

    // region Tun2Socks native methods

    public native void a10(
            int vpnInterfaceFileDescriptor,
            int vpnInterfaceMTU,
            String vpnIpAddress,
            String vpnNetMask,
            String socksServerAddress,
            String udpgwServerAddress,
            int udpgwTransparentDNS);

    public native void a11();

    // endregion

    // region Logger methods

    //TODO: keep name
    public void a12(int priority, String tag, String message)
    {
        if(logProvider != null)
            logProvider.logNativeMessage(priority, tag, message);

        this.a13(this);
    }

    public native void a13(TLJNIBridge bridge);

    // endregion
}

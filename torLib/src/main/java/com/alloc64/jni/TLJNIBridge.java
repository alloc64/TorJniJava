/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package com.alloc64.jni;

import android.os.Looper;
import android.util.Log;

import com.alloc64.http.ProxiedSocketFactory;
import com.alloc64.torlib.PdnsdConfig;
import com.alloc64.torlib.TorConfig;
import com.alloc64.torlib.control.TorAbstractControlSocket;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class TLJNIBridge
{
    public static final String TAG = TLJNIBridge.class.getName();

    public class Tor
    {
        private final List<TorAbstractControlSocket> controlPortSockets = new ArrayList<>();
        private TorControlSocket defaultControlSocket;

        public String getTorVersion()
        {
            ensureMainThread();

            return jniTrampoline.call(TLJNIBridge.this::a1);
        }

        public Tor createTorConfig() throws IllegalStateException
        {
            ensureMainThread();

            if (!jniTrampoline.call(TLJNIBridge.this::a2))
                throw new IllegalStateException("Unable to create transport config.");

            return this;
        }

        public Tor setTorCommandLine(String[] args)
        {
            ensureMainThread();

            if (!jniTrampoline.call(() -> a5(args)))
                throw new IllegalStateException("Unable to set command line arguments.");

            return this;
        }

        public Tor setTorCommandLine(TorConfig torConfig)
        {
            ensureMainThread();

            setTorCommandLine(torConfig.asCommands());
            return this;
        }

        public Tor attachControlPort(InetSocketAddress socketAddress, TorAbstractControlSocket... controlSockets)
        {
            ensureMainThread();

            if (controlPortSockets.size() < 1)
            {
                for (TorAbstractControlSocket s : controlSockets)
                {
                    s.connect(socketAddress);

                    if (s instanceof TorControlSocket)
                        this.defaultControlSocket = (TorControlSocket) s;

                    this.controlPortSockets.add(s);
                }
            }
            else
            {
                Log.i(TAG, "Ignoring control port attachment, control port is already connected.");
            }

            return this;
        }

        public TorControlSocket getControlPortSocket()
        {
            ensureMainThread();

            return defaultControlSocket;
        }

        public void detachControlPort()
        {
            ensureMainThread();

            for (TorAbstractControlSocket s : controlPortSockets)
            {
                try
                {
                    if (s != null)
                        s.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            controlPortSockets.clear();

            this.defaultControlSocket = null;
        }

        public void setNetworkEnabled(boolean isEnabled)
        {
            if (defaultControlSocket != null)
                defaultControlSocket.setNetworkEnabled(isEnabled);
        }

        public void reloadTorNetwork()
        {
            if (defaultControlSocket != null)
                defaultControlSocket.reloadTorNetwork();
        }

        /**
         * Set GEO IP files for exit node targeting by country code and more.
         *
         * @param ipv4
         * @param ipv6
         */
        public void setGeoIPFiles(File ipv4, File ipv6)
        {
            if (defaultControlSocket != null)
            {
                defaultControlSocket.setConf(TorConfig.GEO_IP_FILE, ipv4.getAbsolutePath());
                defaultControlSocket.setConf(TorConfig.GEO_IP_V6_FILE, ipv6.getAbsolutePath());
            }
        }

        /**
         * Set targeting by country code, or by exit node ID.
         * <p>
         * GEO IP files must be set in case you are targeting with country codes.
         * See {@link #setGeoIPFiles(File, File)}.
         *
         * @param exitNodeTargeting
         */
        public void setExitNodeTargeting(List<String> exitNodeTargeting)
        {
            if (defaultControlSocket != null)
                defaultControlSocket.setExitNodeTargeting(exitNodeTargeting);
        }

        public void disableExitNodeTargeting()
        {
            if (defaultControlSocket != null)
                defaultControlSocket.disableExitNodeTargeting();
        }

        /**
         * Start TOR in subthread.
         * <p>
         * Library internaly checks for duplicate starts, so TOR can be started only once.
         *
         * @return
         */
        public Tor startTor()
        {
            ensureMainThread();

            if (isTorRunning())
            {
                Log.d(TAG, "Ignoring start. T is already running.");
            }
            else
            {
                jniTrampoline.call(TLJNIBridge.this::a6);
            }

            return this;
        }

        /**
         * Destroy TOR in subthread.
         * <p>
         * As tor was primarily made as standalone application, use as shared library is a bit quirky.
         * Probably that's why, TOR is mostly run as standalone process, so when crash occurs, nothing bad happens.
         * <p>
         * Sometimes you may experience crashes, which will crash whole application.
         * You know, this happens, mostly in case you destroy TOR in bootstrap phase.
         * <p>
         * These crashes are out of control of this library and can be avoided by having TOR in dormant/active mode.
         *
         * @return
         */
        public void destroyTor() throws IOException
        {
            ensureMainThread();

            jniTrampoline.call(TLJNIBridge.this::a3);

            detachControlPort();
        }

        public boolean isTorRunning()
        {
            ensureMainThread();

            return jniTrampoline.call(TLJNIBridge.this::a4);
        }

        public OkHttpClient.Builder createOkHttpClient(InetSocketAddress socketAddress)
        {
            ensureMainThread();

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
            ensureMainThread();

            if (isPdnsdRunning())
            {
                Log.i(TAG, "Ignoring start. PD is already running.");
            }
            else
            {
                jniTrampoline.call(() -> a8(args));
            }

            return this;
        }

        public Pdnsd startPdnsd(PdnsdConfig config)
        {
            ensureMainThread();

            return startDnsd(config.asCommands());
        }

        public void destroyPdnsd()
        {
            ensureMainThread();

            jniTrampoline.call(TLJNIBridge.this::a9);
        }

        public boolean isPdnsdRunning()
        {
            ensureMainThread();

            return jniTrampoline.call(TLJNIBridge.this::a7);
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
                String udpgwServerAddress)
        {
            ensureMainThread();

            if (isInterfaceRunning())
            {
                Log.i(TAG, "Ignoring start. T2 is already running.");
            }
            else
            {
                jniTrampoline.call(() -> TLJNIBridge.this.a10(vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, vpnNetMask, socksServerAddress, udpgwServerAddress));
            }
        }

        public void destroyInterface()
        {
            ensureMainThread();

            jniTrampoline.call(TLJNIBridge.this::a11);
        }

        public boolean isInterfaceRunning()
        {
            ensureMainThread();

            return jniTrampoline.call(TLJNIBridge.this::a14);
        }
    }

    public interface LogProvider
    {
        void logNativeMessage(int priority, String tag, String message);
    }

    public interface MainThreadDispatcher
    {
        void dispatch(Runnable runnable);
    }

    private static final TLJNIBridge instance = new TLJNIBridge();

    private final JNITrampoline jniTrampoline = new JNITrampoline();

    private final Tor tor = new Tor();
    private final Pdnsd pdnsd = new Pdnsd();
    private final Tun2Socks tun2Socks = new Tun2Socks();
    private LogProvider logProvider;
    private MainThreadDispatcher mainThreadDispatcher;

    private TLJNIBridge()
    {
        System.loadLibrary("transport");

        setLogProvider(Log::println);
        setMainThreadDispatcher(Runnable::run);
    }

    public static TLJNIBridge get()
    {
        ensureMainThread();

        return instance;
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
        ensureMainThread();

        this.logProvider = logProvider;
        this.a13(this);
    }

    public synchronized MainThreadDispatcher getMainThreadDispatcher()
    {
        return mainThreadDispatcher;
    }

    public void setMainThreadDispatcher(MainThreadDispatcher mainThreadDispatcher)
    {
        ensureMainThread();

        this.mainThreadDispatcher = mainThreadDispatcher;
    }

    private static void ensureMainThread()
    {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Bridge must be called only from main thread. Was called from: " + Thread.currentThread());
    }

    // region Tor native methods

    public native String a1();

    public native boolean a2();

    public native void a3();

    public native boolean a4();

    public native boolean a5(String[] args);

    public native void a6();

    // endregion

    // region Dnsd native methods

    public native boolean a7();

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
            String udpgwServerAddress);

    public native void a11();

    public native boolean a14();

    // endregion

    // region Logger methods

    //TODO: keep name
    public void a12(int priority, String tag, String message)
    {
        if (logProvider != null)
            getMainThreadDispatcher().dispatch(() -> logProvider.logNativeMessage(priority, tag, message));
    }

    public native void a13(TLJNIBridge bridge);

    // endregion
}

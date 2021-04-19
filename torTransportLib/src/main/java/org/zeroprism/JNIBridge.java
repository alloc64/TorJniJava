package org.zeroprism;

import android.content.Context;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

public class JNIBridge
{
    public class Tor
    {
        public String getTorVersion()
        {
            return jniTrampoline.call(JNIBridge.this::a1);
        }

        public boolean createTorConfig()
        {
            return jniTrampoline.call(JNIBridge.this::a2);
        }

        public void destroyTorConfig()
        {
            jniTrampoline.call(JNIBridge.this::a3);
        }

        public int setupTorControlSocket()
        {
            return jniTrampoline.call(JNIBridge.this::a4);
        }

        public boolean setTorCommandLine(String[] args)
        {
            return jniTrampoline.call(() -> a5(args));
        }

        public FileDescriptor prepareFileDescriptor(String path)
        {
            return jniTrampoline.call(() -> a6(path));
        }

        public void startTor()
        {
            jniTrampoline.call(JNIBridge.this::a7);
        }
    }

    public class Pdnsd
    {
        public File createPdnsdConf(File fileDir, String torDnsHost, int torDnsPort, String pdnsdHost, int pdnsdPort) throws IOException
        {
            String PDNS_CONF = "global {\n" +
                    "\tperm_cache=0;\n" +
                    "\tcache_dir=\"%3$s\";\n" +
                    "\tserver_port = %5$d;\n" +
                    "\tserver_ip = %4$s;\n" +
                    "\tquery_method=udp_only;\n" +
                    "\tmin_ttl=1m;\n" +
                    "\tmax_ttl=1w;\n" +
                    "\ttimeout=10;\n" +
                    "\tdaemon=on;\n" +
                    "\tpid_file=\"%3$s/pdnsd.pid\";\n" +
                    "\n" +
                    "}\n" +
                    "\n" +
                    "server {\n" +
                    "\tlabel= \"upstream\";\n" +
                    "\tip = %1$s;\n" +
                    "\tport = %2$d;\n" +
                    "\tuptest = none;\n" +
                    "}\n" +
                    "\n" +
                    "rr {\n" +
                    "\tname=localhost;\n" +
                    "\treverse=on;\n" +
                    "\ta=127.0.0.1;\n" +
                    "\towner=localhost;\n" +
                    "\tsoa=localhost,root.localhost,42,86400,900,86400,86400;\n" +
                    "}";

            String conf = String.format(Locale.US,
                    PDNS_CONF,
                    torDnsHost,
                    torDnsPort,
                    fileDir.getCanonicalPath(),
                    pdnsdHost,
                    pdnsdPort
            );

            File fPid = new File(fileDir, pdnsdPort + "pdnsd.conf");

            if (fPid.exists())
                fPid.delete();

            FileOutputStream fos = new FileOutputStream(fPid, false);
            PrintStream ps = new PrintStream(fos);
            ps.print(conf);
            ps.close();

            File cache = new File(fileDir, "pdnsd.cache");

            if (!cache.exists())
                cache.createNewFile();

            return fPid;
        }

        public void startDnsd(String[] args)
        {
            jniTrampoline.call(() -> a8(args));
        }

        public void destroyDnsd()
        {
            jniTrampoline.call(JNIBridge.this::a9);
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
            jniTrampoline.call(() -> JNIBridge.this.a10(vpnInterfaceFileDescriptor, vpnInterfaceMTU, vpnIpAddress, vpnNetMask, socksServerAddress, udpgwServerAddress, udpgwTransparentDNS));
        }

        public void destroyInterface()
        {
            jniTrampoline.call(JNIBridge.this::a11);
        }
    }

    private final JNITrampoline jniTrampoline = new JNITrampoline();

    private final Tor tor = new Tor();
    private final Pdnsd pdnsd = new Pdnsd();
    private final Tun2Socks tun2Socks = new Tun2Socks();

    public Tor getTor()
    {
        return tor;
    }

    public Pdnsd getPdnsd()
    {
        return pdnsd;
    }

    // region Tor native methods

    public native String a1();

    public native boolean a2();

    public native void a3();

    public native int a4();

    public native boolean a5(String[] args);

    public native static FileDescriptor a6(String path);

    public native void a7();

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
}

package org.zeroprism;

import android.content.Context;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

public class Transport
{
    private static final String PDNS_CONF = "global {\n" +
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

    public native String version();

    public native boolean createTorConfiguration();

    public native void mainConfigurationFree();

    public native static FileDescriptor prepareFileDescriptor(String path);

    public native boolean mainConfigurationSetCommandLine(String[] args);

    public native boolean setupControlSocket();

    public native int runMain();

    public native int runDnsd(String[] args);

    public native int destroyDnsd();

    public native int runTun2SocksInterface(
            int vpnInterfaceFileDescriptor,
            int vpnInterfaceMTU,
            String vpnIpAddress,
            String vpnNetMask,
            String socksServerAddress,
            String udpgwServerAddress,
            int udpgwTransparentDNS);

    public native void destroyTun2SocksInterface();

    public void test(Context ctx) throws IOException
    {
        runDnsd(new String[]{"-c", createPdnsdConf(ctx.getFilesDir(), "1.1.1.1", 53, "127.0.0.1", 5353).toString(), "-g", "-v2"});
    }

    private File createPdnsdConf(File fileDir, String torDnsHost, int torDnsPort, String pdnsdHost, int pdnsdPort) throws IOException
    {
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
}

package com.alloc64.torlib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Locale;

public class PdnsdConfig extends ArgConfig
{
    private File baseDir;
    private InetSocketAddress upstreamDnsAddress;
    private InetSocketAddress dnsServerAddress;

    public PdnsdConfig()
    {
        addCommand("exec", "-g", "-v2", "--nodaemon");
    }

    private File createPdnsdConf(File baseDir,
                                 String torDnsHost,
                                 int torDnsPort,
                                 String pdnsdHost,
                                 int pdnsdPort) throws IOException
    {
        String PDNS_CONF = "global {\n" +
                "\tperm_cache=0;\n" +
                "\tcache_dir=\"%s\";\n" +
                "\tserver_ip = %s;\n" +
                "\tserver_port = %d;\n" +
                "\tquery_method=udp_only;\n" +
                "\tmin_ttl=1m;\n" +
                "\tmax_ttl=1w;\n" +
                "\ttimeout=10;\n" +
                "\tdaemon=on;\n" +
                "\n" +
                "}\n" +
                "\n" +
                "server {\n" +
                "\tlabel= \"upstream\";\n" +
                "\tip = %s;\n" +
                "\tport = %d;\n" +
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

        File fPid = new File(baseDir, "pdnsd.conf");

        if (fPid.exists())
            fPid.delete();

        File cache = new File(baseDir, "pdnsd.cache");

        if (!cache.exists())
            cache.createNewFile();

        String conf = String.format(Locale.US, PDNS_CONF,
                cache.getCanonicalPath(),
                pdnsdHost,
                pdnsdPort,
                torDnsHost,
                torDnsPort
        );

        FileOutputStream fos = new FileOutputStream(fPid, false);
        PrintStream ps = new PrintStream(fos);
        ps.print(conf);
        ps.close();

        return fPid;
    }

    public PdnsdConfig setBaseDir(File baseDir)
    {
        this.baseDir = baseDir;
        return this;
    }

    public PdnsdConfig setUpstreamDnsAddress(InetSocketAddress upstreamDnsAddress)
    {
        this.upstreamDnsAddress = upstreamDnsAddress;
        return this;
    }

    public PdnsdConfig setDnsServerAddress(InetSocketAddress dnsServerAddress)
    {
        this.dnsServerAddress = dnsServerAddress;
        return this;
    }

    @Override
    public String[] asCommands()
    {
        if (baseDir == null)
            throw new IllegalStateException("baseDir is mandatory");

        if (upstreamDnsAddress == null)
            throw new IllegalStateException("upstreamDnsAddress is mandatory");

        if (dnsServerAddress == null)
            throw new IllegalStateException("dnsServerAddress is mandatory");

        try
        {
            File conf = createPdnsdConf(baseDir,
                    upstreamDnsAddress.getHostString(),
                    upstreamDnsAddress.getPort(),
                    dnsServerAddress.getHostString(),
                    dnsServerAddress.getPort()
            );

            addCommand("-c", conf.getCanonicalPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return super.asCommands();
    }
}

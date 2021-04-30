package com.alloc64.vpn.tor;

import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.PdnsdConfig;
import com.alloc64.torlib.TorConfig;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorAbstractControlSocket;
import com.alloc64.torlib.control.TorControlSocket;
import com.alloc64.torlib.control.TorEventSocket;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TorVpnProvider
{
    private static final String TAG = TorVpnProvider.class.toString();

    private final static int VPN_MTU = 1500;

    private final String gatewayIp = "192.168.200.1";
    private final String clientIp = "192.168.200.2";
    private final String virtualNetMask = "255.255.255.0";
    private final String defaultRoute = "0.0.0.0";

    private final String dummyDNS = "1.1.1.1"; //this is intercepted by the tun2socks library, but we must put in a valid DNS to start

    private final int socksPort = 9050;
    private final int transPort = 9040;
    private final int dnsPort = 5400;
    private final int controlPort = 9051;
    private final int udpgwPort = 8092;

    public void start(VpnService ctx, VpnService.Builder builder)
    {
        try
        {
            builder.setMtu(VPN_MTU);
            builder.addAddress(gatewayIp, 32);
            builder.addRoute(defaultRoute, 0);
            builder.addDnsServer(dummyDNS); // this is intercepted by the tun2socks library, but we must put in a valid DNS to start
            builder.addRoute(dummyDNS, 32);
            builder.addDisallowedApplication(ctx.getPackageName());
            builder.setConfigureIntent(null);
            builder.setBlocking(false);

            ParcelFileDescriptor tunInterface = builder.setSession("VPN")
                    .establish();

            File filesDir = ctx.getFilesDir();
            File dataDirectory = new File(filesDir, "/transport");
            dataDirectory.mkdir();

            Handler mainThreadHandler = new Handler();

            TLJNIBridge.get().setMainThreadDispatcher(mainThreadHandler::post);

            PasswordDigest controlPortPassword = PasswordDigest.generateDigest();

            /*
            RunAsDaemon 1
            AvoidDiskWrites 0
            ControlPort auto
            SOCKSPort 0
            DNSPort 0
            TransPort 0
            CookieAuthentication 1
            DisableNetwork 1


            ControlPortWriteToFile /data/data/org.torproject.android/files/control.txt
            PidFile /data/data/org.torproject.android/files/torpid
            RunAsDaemon 0
            AvoidDiskWrites 0
            SOCKSPort 9050 IPv6Traffic PreferIPv6
            SafeSocks 0
            TestSocks 0
            HTTPTunnelPort 8118
            ReducedConnectionPadding 1
            CircuitPadding 1
            ReducedCircuitPadding 1
            TransPort 9040
            DNSPort 5400
            VirtualAddrNetwork 10.192.0.0/10
            AutomapHostsOnResolve 1
            DormantClientTimeout 10 minutes
            DormantCanceledByStartup 1
            DisableNetwork 0
            UseBridges 0
            GeoIPFile /data/data/org.torproject.android/files/geoip
            GeoIPv6File /data/data/org.torproject.android/files/geoip6
            StrictNodes 0
            ClientOnionAuthDir /data/user/0/org.torproject.android/files/v3_client_auth

            pdsnd conf:global {
            perm_cache=0;
            cache_dir=/data/data/org.torproject.android/files;
            server_port = 8092;
            server_ip = 192.168.200.1;
            query_method=udp_only;
            min_ttl=1m;
            max_ttl=1w;
            timeout=10;
            daemon=on;
            pid_file=/data/data/org.torproject.android/files/pdnsd.pid;
            }

            server {
            label= upstream;
            ip = 127.0.0.1;
            port = 5400;
            uptest = none;

            } rr { name=localhost; reverse=on; a=127.0.0.1; owner=localhost; soa=localhost,root.localhost,42,86400,900,86400,86400; }
            */

            InetSocketAddress controlPortAddress = InetSocketAddress.createUnresolved("127.0.0.1", controlPort);

            TLJNIBridge
                    .get()
                    .getTor()
                    .createTorConfig()
                    .setTorCommandLine(new TorConfig()
                            .addAllowMissingTorrc()
                            .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                            .setRunAsDaemon(false)
                            .addCommandPrefixed("AvoidDiskWrites", "0")
                            .setControlPort(controlPortAddress)
                            .setSocksPort(socksPort + " IPv6Traffic PreferIPv6")
                            .addCommandPrefixed("SafeSocks", "0")
                            .addCommandPrefixed("TestSocks", "0")
                            .addCommandPrefixed("HTTPTunnelPort", "8118")
                            .addCommandPrefixed("ReducedConnectionPadding", "1")
                            .addCommandPrefixed("CircuitPadding", "1")
                            .setTransPort(String.valueOf(transPort))
                            .setDnsPort(String.valueOf(dnsPort))
                            .addCommandPrefixed("VirtualAddrNetwork", "10.192.0.0/10")
                            .addCommandPrefixed("AutomapHostsOnResolve", "1")
                            .addCommandPrefixed("DormantClientTimeout", "10 minutes")
                            .addCommandPrefixed("DormantOnFirstStartup", "0")
                            .addCommandPrefixed("DormantCanceledByStartup", "1")
                            .setDisableNetwork("1")
                            .setSafeLogging("0")
                            .setUseBridges(false)
                            //.addCommandPrefixed("GeoIPFile", "/data/data/org.torproject.android/files/geoip")
                            //.addCommandPrefixed("GeoIPv6File", "/data/data/org.torproject.android/files/geoip6")
                            .addCommandPrefixed("StrictNodes", "0")
                            .setHashedControlPassword(controlPortPassword)
                            .setDataDirectory(dataDirectory))
                    .startTor()
                    .attachControlPort(controlPortAddress, new TorControlSocket(controlPortPassword, new TorControlSocket.ConnectionHandler()
                    {
                        @Override
                        public void onConnectedAsync(TorControlSocket socket)
                        {
                            onTorInitializedAsync(ctx, socket, dataDirectory, tunInterface);
                        }

                        @Override
                        public void onException(TorControlSocket socket, Exception e)
                        {
                            TorVpnProvider.this.onException(e);
                        }
                    }), new TorEventSocket(controlPortPassword, Arrays.asList("CIRC", "STREAM", "ORCONN", "BW", "NOTICE", "ERR", "NEWDESC", "ADDRMAP"), new TorEventSocket.EventHandler()
                    {
                        @Override
                        public void onEvent(TorEventSocket socket, List<TorControlSocket.Reply> replyList)
                        {
                            for(TorControlSocket.Reply r : replyList)
                                Log.i(TAG, "Received TOR event: " + r.getMessage());

                            System.currentTimeMillis();
                        }

                        @Override
                        public void onException(TorEventSocket socket, Exception e)
                        {
                            TorVpnProvider.this.onException(e);
                        }
                    }));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Called from another thread
     *
     * @param ctx
     * @param socket
     * @param dataDirectory
     * @param tunInterface
     */
    private void onTorInitializedAsync(VpnService ctx, TorControlSocket socket, File dataDirectory, ParcelFileDescriptor tunInterface)
    {
        TLJNIBridge.get().getTor().setNetworkEnabled(true);

        //socket.signal(TorAbstractControlSocket.Signal.DEBUG);

        TLJNIBridge
                .get()
                .getMainThreadDispatcher()
                .dispatch(() ->
        {
            TLJNIBridge
                    .get()
                    .getPdnsd()
                    .startPdnsd(new PdnsdConfig()
                            .setBaseDir(dataDirectory)
                            .setUpstreamDnsAddress(InetSocketAddress.createUnresolved("127.0.0.1", dnsPort))
                            .setDnsServerAddress(InetSocketAddress.createUnresolved(gatewayIp, udpgwPort))
                    );

            String socksAddress = String.format(Locale.US, "127.0.0.1:%d", socksPort);
            String dnsAddress = String.format(Locale.US, "%s:%d", gatewayIp, udpgwPort);

            TLJNIBridge
                    .get()
                    .getTun2Socks()
                    .createInterface(
                            tunInterface.detachFd(),
                            VPN_MTU,
                            clientIp,
                            virtualNetMask,
                            socksAddress,
                            dnsAddress);
        });
    }

    private void onException(Exception e)
    {
        e.printStackTrace();
    }
}

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

    String gatewayIp = "192.168.200.1";
    String clientIp = "192.168.200.2";
    int gatewaySocksPort = 9055;

    private InetSocketAddress torSocksAddress = InetSocketAddress.createUnresolved("0.0.0.0", 9050);
    private InetSocketAddress controlPortAddress = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
    private InetSocketAddress dnsAddress = InetSocketAddress.createUnresolved("0.0.0.0", 9053);
    private InetSocketAddress cachingDnsAddress = InetSocketAddress.createUnresolved(gatewayIp, 9054);

    private SocksServerTunInterface socksServerTunInterface = new SocksServerTunInterface(gatewaySocksPort);

    public void start(VpnService ctx, VpnService.Builder builder)
    {
        try
        {
            builder.setMtu(VPN_MTU);
            builder.addAddress(gatewayIp, 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1"); // this is intercepted by the tun2socks library, but we must put in a valid DNS to start
            builder.addDisallowedApplication(ctx.getPackageName());
            builder.setConfigureIntent(null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                builder.setBlocking(true);

            ParcelFileDescriptor tunInterface = builder.setSession("VPN")
                    .establish();

            File filesDir = ctx.getFilesDir();
            File dataDirectory = new File(filesDir, "/transport");
            dataDirectory.mkdir();

            Handler mainThreadHandler = new Handler();

            TLJNIBridge.get().setMainThreadDispatcher(mainThreadHandler::post);

            try
            {
                socksServerTunInterface.start(ctx);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            PasswordDigest controlPortPassword = PasswordDigest.generateDigest();

            TLJNIBridge
                    .get()
                    .getTor()
                    .createTorConfig()
                    .setTorCommandLine(new TorConfig()
                            .addAllowMissingTorrc()
                            .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                            .setRunAsDaemon(false)
                            .setControlPort(controlPortAddress)
                            .setSocksPort(torSocksAddress)
                            .setDnsPort(dnsAddress)
                            .setSafeLogging("0")
                            .setSocks5Proxy("127.0.0.1:" + gatewaySocksPort)
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
        TLJNIBridge.Tor tor = TLJNIBridge.get().getTor();

        TLJNIBridge.get().getMainThreadDispatcher().dispatch(() ->
        {
            TLJNIBridge
                    .get()
                    .getPdnsd()
                    .startPdnsd(new PdnsdConfig()
                            .setBaseDir(dataDirectory)
                            .setUpstreamDnsAddress(InetSocketAddress.createUnresolved(gatewayIp, dnsAddress.getPort()))
                            .setDnsServerAddress(cachingDnsAddress)
                    );

            String socksAddress = String.format(Locale.US, "%s:%d", "127.0.0.1", torSocksAddress.getPort());
            String dnsAddress = String.format(Locale.US, "%s:%d", gatewayIp, cachingDnsAddress.getPort());

            TLJNIBridge
                    .get()
                    .getTun2Socks()
                    .createInterface(
                            tunInterface.detachFd(),
                            VPN_MTU,
                            clientIp,
                            "255.255.255.0",
                            socksAddress,
                            dnsAddress);
        });
    }

    private void onException(Exception e)
    {
        e.printStackTrace();
    }
}

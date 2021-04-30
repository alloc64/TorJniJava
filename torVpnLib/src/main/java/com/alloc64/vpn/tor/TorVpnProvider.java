package com.alloc64.vpn.tor;

import android.content.Context;
import android.net.VpnService;
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
import com.alloc64.torlib.utils.TorUtils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class TorVpnProvider
{
    private static final String TAG = TorVpnProvider.class.toString();

    private static class PortConfiguration
    {
        private int socksPort;
        private int controlPort;
        private int dnsPort;

        public int getSocksPort()
        {
            return socksPort;
        }

        public void setSocksPort(int socksPort)
        {
            this.socksPort = socksPort;
        }

        public int getControlPort()
        {
            return controlPort;
        }

        public void setControlPort(int controlPort)
        {
            this.controlPort = controlPort;
        }

        public int getDnsPort()
        {
            return dnsPort;
        }

        public void setDnsPort(int dnsPort)
        {
            this.dnsPort = dnsPort;
        }
    }

    private final static int VPN_MTU = 1500;

    private final String gatewayIp = "192.168.200.1";
    private final String clientIp = "192.168.200.2";
    private final String virtualNetMask = "255.255.255.0";

    private final Handler mainThreadHandler = new Handler();

    private void configure()
    {
    }

    public void start(VpnService ctx, String sessionName, VpnService.Builder builder)
    {
        Executors.newSingleThreadExecutor().submit(() ->
        {
            PortConfiguration config = new PortConfiguration();
            config.setSocksPort(TorUtils.checkLocalPort(9050));
            config.setControlPort(TorUtils.checkLocalPort(9051));
            config.setDnsPort(TorUtils.checkLocalPort(5400));

            mainThreadHandler.post(() -> setupInterfaceWithTor(ctx, sessionName, config, builder));
        });
    }

    private void setupInterfaceWithTor(VpnService ctx, String sessionName, PortConfiguration config, VpnService.Builder builder)
    {
        try
        {
            ParcelFileDescriptor tunInterface = builder
                    .setMtu(VPN_MTU)
                    .addAddress(gatewayIp, 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDisallowedApplication(ctx.getPackageName())
                    .setConfigureIntent(null)
                    .setBlocking(false)
                    .setSession(sessionName)
                    .establish();

            File filesDir = ctx.getFilesDir();
            File dataDirectory = new File(filesDir, "/transport");
            dataDirectory.mkdir();

            File geoipFile = new File(dataDirectory, "geoip");
            assetToFile(ctx, geoipFile, "geoip");

            File geoip6File = new File(dataDirectory, "geoip6");
            assetToFile(ctx, geoip6File, "geoip6");

            TLJNIBridge.get().setMainThreadDispatcher(mainThreadHandler::post);

            PasswordDigest controlPortPassword = PasswordDigest.generateDigest();

            InetSocketAddress controlPortAddress = InetSocketAddress.createUnresolved("127.0.0.1", config.getControlPort());

            TLJNIBridge
                    .get()
                    .getTor()
                    .createTorConfig()
                    .setTorCommandLine(new TorConfig()
                            .addAllowMissingTorrc()
                            .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                            .setRunAsDaemon(false)
                            .setControlPort(controlPortAddress)
                            .setSocksPort(config.getSocksPort() + " IPv6Traffic PreferIPv6")
                            .setDnsPort(String.valueOf(config.getDnsPort()))
                            .addCommandPrefixed("AvoidDiskWrites", "0")
                            .addCommandPrefixed("SafeSocks", "0")
                            .addCommandPrefixed("TestSocks", "0")
                            .addCommandPrefixed("ReducedConnectionPadding", "1")
                            .addCommandPrefixed("CircuitPadding", "1")
                            .addCommandPrefixed("StrictNodes", "0")
                            .setDisableNetwork(true)
                            .setSafeLogging("0")
                            .setUseBridges(false)
                            .addCommandPrefixed("GeoIPFile", geoipFile.getAbsolutePath())
                            .addCommandPrefixed("GeoIPv6File", geoip6File.getAbsolutePath())
                            .setHashedControlPassword(controlPortPassword)
                            .setDataDirectory(dataDirectory))
                    .startTor()
                    .attachControlPort(controlPortAddress, new TorControlSocket(controlPortPassword, new TorControlSocket.ConnectionHandler()
                    {
                        @Override
                        public void onConnectedAsync(TorControlSocket socket)
                        {
                            onTorInitializedAsync(config, socket, dataDirectory, tunInterface);
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
                            for (TorControlSocket.Reply r : replyList)
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
     */
    private void onTorInitializedAsync(
            PortConfiguration portConfiguration,
            TorControlSocket socket,
            File dataDirectory,
            ParcelFileDescriptor tunInterface)
    {
        socket.setNetworkEnabled(true);
        //socket.signal(TorAbstractControlSocket.Signal.DEBUG);

        int udpgwPort = TorUtils.checkLocalPort(8092);

        TLJNIBridge
                .get()
                .getMainThreadDispatcher()
                .dispatch(() ->
                {
                    TLJNIBridge bridge = TLJNIBridge
                            .get();

                    bridge.getPdnsd()
                            .startPdnsd(new PdnsdConfig()
                                    .setBaseDir(dataDirectory)
                                    .setUpstreamDnsAddress(InetSocketAddress.createUnresolved("127.0.0.1", portConfiguration.getDnsPort()))
                                    .setDnsServerAddress(InetSocketAddress.createUnresolved(gatewayIp, udpgwPort))
                            );

                    bridge.getTun2Socks()
                            .createInterface(
                                    tunInterface.detachFd(),
                                    VPN_MTU,
                                    clientIp,
                                    virtualNetMask,
                                    String.format(Locale.US, "127.0.0.1:%d", portConfiguration.getSocksPort()),
                                    String.format(Locale.US, "%s:%d", gatewayIp, udpgwPort));
                });
    }

    private void onException(Exception e)
    {
        e.printStackTrace();
    }

    private void assetToFile(Context ctx, File targetFile, String path) throws IOException
    {
        if (targetFile.exists())
            return;

        IOUtils.copy(ctx.getAssets().open(path), new FileOutputStream(targetFile));
    }
}

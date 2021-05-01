package com.alloc64.vpn.tor;

import android.content.Context;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.PdnsdConfig;
import com.alloc64.torlib.TorConfig;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;
import com.alloc64.torlib.control.TorEventSocket;
import com.alloc64.torlib.utils.TorUtils;
import com.alloc64.vpn.BuildConfig;
import com.alloc64.vpn.VpnError;
import com.alloc64.vpn.VpnException;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TorVpnProvider
{
    private static final String TAG = TorVpnProvider.class.toString();
    private static final long CONNECTION_TIMEOUT = 20 * 1000;
    private final static int VPN_MTU = 1500;

    public static class VpnConfiguration
    {
        private final VpnService.Builder vpnBuilder;
        private String gatewayIp;
        private String clientIp;
        private String clientIpMask;

        public VpnConfiguration(VpnService.Builder vpnBuilder)
        {
            this.vpnBuilder = vpnBuilder;
        }

        public VpnService.Builder getVpnBuilder()
        {
            return vpnBuilder;
        }

        public VpnConfiguration setSessionName(String sessionName)
        {
            vpnBuilder.setSession(sessionName);
            return this;
        }

        public String getGatewayIp()
        {
            return gatewayIp;
        }

        public VpnConfiguration setGatewayIp(String gatewayIp)
        {
            this.gatewayIp = gatewayIp;
            return this;
        }

        public String getClientIp()
        {
            return clientIp;
        }

        public VpnConfiguration setClientIp(String clientIp)
        {
            this.clientIp = clientIp;
            return this;
        }

        public String getClientIpMask()
        {
            return clientIpMask;
        }

        public VpnConfiguration setClientIpMask(String clientIpMask)
        {
            this.clientIpMask = clientIpMask;
            return this;
        }
    }

    private static class PortConfiguration
    {
        private int socksPort;
        private int controlPort;
        private int dnsPort;
        private int udpgwPort;

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

        public int getUdpgwPort()
        {
            return udpgwPort;
        }

        public void setUdpgwPort(int udpgwPort)
        {
            this.udpgwPort = udpgwPort;
        }
    }

    private final VpnService ctx;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final File dataDirectory;
    private final PortConfiguration portConfig = new PortConfiguration();
    private ParcelFileDescriptor tunInterface;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private boolean pdnsPortsAssigned = false;
    private ScheduledFuture<?> pendingConnectionCheck;

    public TorVpnProvider(VpnService ctx)
    {
        this.ctx = ctx;
        this.dataDirectory = new File(ctx.getFilesDir(), "transport");
        dataDirectory.mkdir();
    }

    public void connect(VpnConfiguration vpnConfiguration)
    {
        onConnecting();

        executor.execute(() ->
        {
            try
            {
                portConfig.setSocksPort(TorUtils.checkLocalPort(9050));
                portConfig.setControlPort(TorUtils.checkLocalPort(9051));

                if (!pdnsPortsAssigned)
                {
                    pdnsPortsAssigned = true;
                    // choose ports once, use the same ports all app lifetime
                    portConfig.setDnsPort(TorUtils.checkLocalPort(5400));
                    portConfig.setUdpgwPort(TorUtils.checkLocalPort(8092));
                }

                mainThreadHandler.post(() -> setupInterfaceWithTor(vpnConfiguration));
            }
            catch (Exception e)
            {
                onException(e);
            }
        });
    }

    public void disconnect()
    {
        try
        {
            TLJNIBridge bridge = TLJNIBridge.get();

            if (!bridge.getTor().isTorRunning())
                return;

            executor.execute(() -> bridge.getTor().setNetworkEnabled(false));

            bridge.getTun2Socks().destroyInterface();

            try
            {
                if (tunInterface != null)
                    tunInterface.close();
            }
            catch (Exception e)
            {
                onException(e);
            }

            onDisconnected();
        }
        catch (Exception e)
        {
            onException(e);
        }
    }

    private void setupInterfaceWithTor(VpnConfiguration vpnConfiguration)
    {
        try
        {
            this.tunInterface = vpnConfiguration
                    .getVpnBuilder()
                    .setMtu(VPN_MTU)
                    .addAddress(vpnConfiguration.getGatewayIp(), 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDisallowedApplication(ctx.getPackageName())
                    .setConfigureIntent(null)
                    .setBlocking(false)
                    .establish();

            File geoipFile = new File(dataDirectory, "geoip");
            assetToFile(ctx, geoipFile, "geoip");

            File geoip6File = new File(dataDirectory, "geoip6");
            assetToFile(ctx, geoip6File, "geoip6");

            TLJNIBridge bridge = TLJNIBridge
                    .get();

            bridge.setMainThreadDispatcher(mainThreadHandler::post);

            PasswordDigest controlPortPassword = PasswordDigest.generateDigest();
            InetSocketAddress controlPortAddress = InetSocketAddress.createUnresolved("127.0.0.1", portConfig.getControlPort());

            TorConfig torConfig = new TorConfig()
                    .addAllowMissingTorrc()
                    .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                    .setRunAsDaemon(false)
                    .setControlPort(controlPortAddress)
                    .setSocksPort(portConfig.getSocksPort() + " IPv6Traffic PreferIPv6")
                    .setDnsPort(String.valueOf(portConfig.getDnsPort()))
                    .addCommandPrefixed("AvoidDiskWrites", "0")
                    .addCommandPrefixed("SafeSocks", "0")
                    .addCommandPrefixed("TestSocks", "0")
                    .addCommandPrefixed("ReducedConnectionPadding", "1")
                    .addCommandPrefixed("CircuitPadding", "1")
                    .addCommandPrefixed("StrictNodes", "0")
                    .setDisableNetwork(true)
                    .setUseBridges(false)
                    .setGeoIPFiles(geoipFile, geoip6File)
                    .setHashedControlPassword(controlPortPassword)
                    .setDataDirectory(dataDirectory);

            if (BuildConfig.DEBUG)
                torConfig.setSafeLogging("0");

            TLJNIBridge.Tor tor = bridge.getTor();

            if (tor.isTorRunning())
            {
                TorControlSocket controlPort = tor.getControlPortSocket();

                executor.execute(() -> enableTunInterfaceAsync(controlPort, vpnConfiguration, tunInterface));
            }
            else
            {
                tor.createTorConfig()
                        .setTorCommandLine(torConfig)
                        .startTor()
                        .attachControlPort(controlPortAddress, new TorControlSocket(controlPortPassword, new TorControlSocket.ConnectionHandler()
                        {
                            @Override
                            public void onConnectedAsync(TorControlSocket socket)
                            {
                                enableTunInterfaceAsync(socket, vpnConfiguration, tunInterface);
                            }

                            @Override
                            public void onException(TorControlSocket socket, Exception e)
                            {
                                TorVpnProvider.this.onException(e);
                            }
                        }, mainThreadHandler::post), new TorEventSocket(controlPortPassword, Arrays.asList("ORCONN", "BW", "NOTICE", "ERR"), new TorEventSocket.EventHandler()
                        {
                            @Override
                            public void onEvent(TorEventSocket socket, List<TorControlSocket.Reply> replyList)
                            {
                                for (TorControlSocket.Reply r : replyList)
                                    Log.i(TAG, "Received TOR event: " + r.getMessage());
                            }

                            @Override
                            public void onException(TorEventSocket socket, Exception e)
                            {
                                TorVpnProvider.this.onException(e);
                            }
                        }, mainThreadHandler::post));
            }
        }
        catch (Exception e)
        {
            onException(e);
        }
    }

    private void enableTunInterfaceAsync(TorControlSocket socket, VpnConfiguration vpnConfiguration, ParcelFileDescriptor tunInterface)
    {
        socket.setNetworkEnabled(true);
        //socket.signal(TorAbstractControlSocket.Signal.DEBUG);

        startPendingConnectionCheck(socket);

        mainThreadHandler.post(() ->
        {
            try
            {
                if (!TLJNIBridge.get()
                        .getPdnsd()
                        .isPdnsdRunning())
                {
                    /*
                     * Start PDNSd
                     * This daemon implementation does not allow restart without crashing/exiting main process, so it is started once per app lifetime for now...
                     */
                    TLJNIBridge.get()
                            .getPdnsd()
                            .startPdnsd(new PdnsdConfig()
                                    .setBaseDir(dataDirectory)
                                    .setUpstreamDnsAddress(InetSocketAddress.createUnresolved("127.0.0.1", portConfig.getDnsPort()))
                                    .setDnsServerAddress(InetSocketAddress.createUnresolved(vpnConfiguration.getGatewayIp(), portConfig.getUdpgwPort()))
                            );
                }

                TLJNIBridge
                        .get()
                        .getTun2Socks()
                        .createInterface(
                                tunInterface.detachFd(),
                                VPN_MTU,
                                vpnConfiguration.getClientIp(),
                                vpnConfiguration.getClientIpMask(),
                                String.format(Locale.US, "127.0.0.1:%d", portConfig.getSocksPort()),
                                String.format(Locale.US, "%s:%d", vpnConfiguration.getGatewayIp(), portConfig.getUdpgwPort())
                        );
            }
            catch (Exception e)
            {
                onException(e);
            }
        });
    }

    //TODO: dispatch callbacks
    private void onConnecting()
    {
    }

    private void onConnected()
    {
    }

    private void onDisconnected()
    {

    }

    private void onException(Exception exception)
    {
        VpnException e;

        if (exception instanceof VpnException)
            e = (VpnException) exception;
        else
            e = new VpnException(VpnError.FatalException, exception.getMessage(), exception);

        //TODO: report e.getError()
        e.printStackTrace();
    }

    private void assetToFile(Context ctx, File targetFile, String path) throws IOException
    {
        if (targetFile.exists())
            return;

        IOUtils.copy(ctx.getAssets().open(path), new FileOutputStream(targetFile));
    }

    private void startPendingConnectionCheck(TorControlSocket socket)
    {
        stopPendingConnectionCheck();

        this.pendingConnectionCheck = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Runnable()
        {
            private final long startTimestamp = System.currentTimeMillis();
            
            @Override
            public void run()
            {
                Map<String, String> info = socket.getInfo(Arrays.asList(
                        TorEventSocket.Event.STATUS_ENOUGH_DIR_INFO,
                        TorEventSocket.Event.STATUS_REACHABILITY_SUCCEEDED_DIR,
                        TorEventSocket.Event.NETWORK_LIVENESS)
                );

                boolean hasEnoughDirInfo = "1".equals(info.get(TorEventSocket.Event.STATUS_ENOUGH_DIR_INFO));
                boolean reachabilitySucceededDir = "1".equals(info.get(TorEventSocket.Event.STATUS_REACHABILITY_SUCCEEDED_DIR));
                boolean isNetworkAlive = "up".equals(info.get(TorEventSocket.Event.NETWORK_LIVENESS));

                if(hasEnoughDirInfo && reachabilitySucceededDir && isNetworkAlive)
                {
                    stopPendingConnectionCheck();

                    mainThreadHandler.post(() -> onConnected());
                }
                else if(System.currentTimeMillis() - startTimestamp >= CONNECTION_TIMEOUT)
                {
                    stopPendingConnectionCheck();
                    mainThreadHandler.post(() -> disconnect());

                    onException(new VpnException(VpnError.ConnectionTimeout, "Connection timeout."));
                }
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    private void stopPendingConnectionCheck()
    {
        if(pendingConnectionCheck != null)
        {
            pendingConnectionCheck.cancel(true);
            this.pendingConnectionCheck = null;
        }
    }
}

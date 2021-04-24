package com.alloc64.vpn.tor;

import android.content.Context;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.PdnsdConfig;
import com.alloc64.torlib.TorConfig;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Locale;

public class TorVpnProvider
{
    private final static int VPN_MTU = 1500;

    String gatewayIp = "192.168.200.1";
    String clientIp = "192.168.200.2";

    private InetSocketAddress socksPort = InetSocketAddress.createUnresolved("127.0.0.1", 9050);
    private InetSocketAddress controlPort = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
    private InetSocketAddress dnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 9053);
    private InetSocketAddress cachingDnsPort = InetSocketAddress.createUnresolved(gatewayIp, 15053);

    public void start(Context ctx, VpnService.Builder builder)
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

            PasswordDigest controlPortPassword = PasswordDigest.generateDigest();

            TLJNIBridge
                    .get()
                    .getTor()
                    .createTorConfig()
                    .setTorCommandLine(new TorConfig()
                            .addAllowMissingTorrc()
                            .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                            .setRunAsDaemon(false)
                            .setSocksPort(socksPort)
                            .setDnsPort(dnsPort)
                            .setSafeLogging("0")
                            .setControlPort(controlPort)
                            .setHashedControlPassword(controlPortPassword)
                            .addCommand("SafeSocks", "0")
                            .addCommand("TestSocks", "0")
                            .setDataDirectory(dataDirectory))
                    .startTor()
                    .attachControlPort(controlPort, controlPortPassword, new TorControlSocket.TorEventHandler()
                    {
                        @Override
                        public void onConnected(TorControlSocket socket)
                        {
                            onTorInitialized(dataDirectory, tunInterface);
                        }

                        @Override
                        public void onException(TorControlSocket socket, Exception e)
                        {
                            e.printStackTrace();
                        }
                    });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void onTorInitialized(File dataDirectory, ParcelFileDescriptor tunInterface)
    {
        TLJNIBridge
                .get()
                .getPdnsd()
                .startPdnsd(new PdnsdConfig()
                        .setBaseDir(dataDirectory)
                        .setUpstreamDnsAddress(dnsPort)
                        .setDnsServerAddress(cachingDnsPort)
                );

        String socksAddress = String.format(Locale.US, "%s:%d", socksPort.getHostString(), socksPort.getPort());
        String dnsAddress = String.format(Locale.US, "%s:%d", gatewayIp, dnsPort.getPort());

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
    }
}

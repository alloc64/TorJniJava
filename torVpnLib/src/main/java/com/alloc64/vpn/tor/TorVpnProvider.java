package com.alloc64.vpn.tor;

import android.content.Context;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.PdnsdConfig;
import com.alloc64.torlib.TorConfig;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.net.InetSocketAddress;

public class TorVpnProvider
{
    private final static int VPN_MTU = 1500;

    private final TLJNIBridge bridge = new TLJNIBridge();

    private InetSocketAddress socksPort = InetSocketAddress.createUnresolved("127.0.0.1", 9050);
    private InetSocketAddress controlPort = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
    private InetSocketAddress dnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 9053);
    private InetSocketAddress cachingDnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 15053);

    public void start(Context ctx, VpnService.Builder builder)
    {
        try
        {
            File filesDir = ctx.getFilesDir();
            File dataDirectory = new File(filesDir, "/transport");
            dataDirectory.mkdir();

            TLJNIBridge.Tor tor = bridge.getTor();

            tor.createTorConfig()
                    .setTorCommandLine(new TorConfig()
                            .addAllowMissingTorrc()
                            .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                            .setRunAsDaemon(false)
                            .setSocksPort(socksPort)
                            .setDnsPort(dnsPort)
                            .setSafeLogging("0")
                            .setControlPort(controlPort)
                            .setDataDirectory(dataDirectory))
                    .startTor()
                    .attachControlPort(controlPort, PasswordDigest.generateDigest(), new TorControlSocket.TorEventHandler()
                    {
                        @Override
                        public void onConnected(TorControlSocket socket)
                        {
                            socket.send("GETINFO status/bootstrap-phase\r\n", new TorControlSocket.Callback()
                            {
                                @Override
                                public void onResult(TorControlSocket socket, TorControlSocket.Reply reply)
                                {
                                    System.currentTimeMillis();

                                }

                            });
                        }

                        @Override
                        public void onException(TorControlSocket socket, Exception e)
                        {
                            e.printStackTrace();
                        }
                    });

            bridge.getPdnsd()
                    .startDnsd(new PdnsdConfig()
                            .setBaseDir(dataDirectory)
                            .setUpstreamDnsAddress(dnsPort)
                            .setDnsServerAddress(cachingDnsPort)
                    );

            //

            String clientIp = "172.0.21.1";

            builder.setMtu(VPN_MTU);
            builder.addAddress(clientIp, 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1"); // this is intercepted by the tun2socks library, but we must put in a valid DNS to start
            builder.addDisallowedApplication(ctx.getPackageName());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                builder.setBlocking(true);

            ParcelFileDescriptor tunInterface = builder.setSession("VPN")
                    .establish();

            bridge.getTun2Socks()
                    .createInterface(
                            tunInterface.detachFd(),
                            VPN_MTU,
                            clientIp,
                            "255.255.255.0",
                            String.format("%s:%d", socksPort.getHostString(), socksPort.getPort()),
                            String.format("%s:%d", clientIp, dnsPort.getPort()),
                            1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

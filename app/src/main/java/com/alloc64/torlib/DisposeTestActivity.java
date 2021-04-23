package com.alloc64.torlib;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.TextView;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;

public class DisposeTestActivity extends Activity
{
    private final InetSocketAddress socksPort = InetSocketAddress.createUnresolved("127.0.0.1", 9050);
    private final InetSocketAddress controlPort = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
    private final InetSocketAddress dnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 9053);

    private File dataDirectory;

    private TextView torStatus;
    private TextView pdnsdStatus;
    private TextView tun2SocksStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dispose_test);

        File filesDir = getFilesDir();
        this.dataDirectory = new File(filesDir, "/transport");
        dataDirectory.mkdir();

        Handler mainThreadHandler = new Handler();

        TLJNIBridge.get().setMainThreadDispatcher(mainThreadHandler::post);

        this.torStatus = findViewById(R.id.tor_status);
        findViewById(R.id.start_tor).setOnClickListener(v -> startTor());
        findViewById(R.id.stop_tor).setOnClickListener(v -> stopTor());

        this.pdnsdStatus = findViewById(R.id.pdnsd_status);
        findViewById(R.id.start_pdnsd).setOnClickListener(v -> startPdnsd());
        findViewById(R.id.stop_pdnsd).setOnClickListener(v -> stopPdnsd());

        this.tun2SocksStatus = findViewById(R.id.tun2socks_status);
        findViewById(R.id.start_tun2socks).setOnClickListener(v -> startTun2Socks());
        findViewById(R.id.stop_tun2socks).setOnClickListener(v -> stopTun2Socks());
    }

    private void startTor()
    {
        if (!TLJNIBridge.get().getTor().isTorRunning())
            torStatus.setText("Tor is starting");

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
                        .setDataDirectory(dataDirectory))
                .startTor()
                .attachControlPort(controlPort, PasswordDigest.generateDigest(), new TorControlSocket.TorEventHandler()
                {
                    @Override
                    public void onConnected(TorControlSocket socket)
                    {
                        socket.send("GETINFO status/bootstrap-phase\r\n", (socket1, reply) -> torStatus.setText(String.format("Tor is in state: %s", reply.getMessage())));
                    }

                    @Override
                    public void onException(TorControlSocket socket, Exception e)
                    {
                        e.printStackTrace();
                    }
                });
    }

    private void stopTor()
    {
        try
        {
            TLJNIBridge
                    .get()
                    .getTor()
                    .destroyTor();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        torStatus.setText("Tor is destroyed");
    }

    private void startPdnsd()
    {
        if (!TLJNIBridge.get().getPdnsd().isPdnsdRunning())
            pdnsdStatus.setText("PDNSd is starting");

        TLJNIBridge
                .get()
                .getPdnsd()
                .startPdnsd(new PdnsdConfig()
                        .setBaseDir(dataDirectory)
                        .setUpstreamDnsAddress(dnsPort)
                        .setDnsServerAddress(new InetSocketAddress("127.0.0.1", 15053))
                );

        if (TLJNIBridge.get().getPdnsd().isPdnsdRunning())
            pdnsdStatus.setText("PDNSd should be running now");
    }

    private void stopPdnsd()
    {
        TLJNIBridge
                .get()
                .getPdnsd()
                .destroyPdnsd();

        pdnsdStatus.setText("PDNSd is destroyed");
    }

    private void startTun2Socks()
    {
        if (!TLJNIBridge.get().getTun2Socks().isInterfaceRunning())
            pdnsdStatus.setText("TUN2Socks is starting");

        int VPN_MTU = 1500;

        String clientIp = "172.0.21.1";

        try
        {
            File f = new File(getFilesDir(), "test.tun");
            f.createNewFile();

            ParcelFileDescriptor tunInterface = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE);

            TLJNIBridge
                    .get()
                    .getTun2Socks()
                    .createInterface(
                            tunInterface.detachFd(),
                            VPN_MTU,
                            clientIp,
                            "255.255.255.0",
                            String.format("%s:%d", socksPort.getHostString(), socksPort.getPort()),
                            String.format("%s:%d", clientIp, dnsPort.getPort()));

            if (TLJNIBridge.get().getTun2Socks().isInterfaceRunning())
                pdnsdStatus.setText("TUN2Socks should be running now");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void stopTun2Socks()
    {
        TLJNIBridge
                .get()
                .getTun2Socks()
                .destroyInterface();

        pdnsdStatus.setText("TUN2Socks is destroyed");
    }
}
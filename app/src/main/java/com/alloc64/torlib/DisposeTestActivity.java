package com.alloc64.torlib;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class DisposeTestActivity extends Activity
{
    private TextView torStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dispose_test);

        Handler mainThreadHandler = new Handler();

        TLJNIBridge.get().setMainThreadDispatcher(mainThreadHandler::post);

        this.torStatus = findViewById(R.id.tor_status);

        findViewById(R.id.start_tor).setOnClickListener(v ->
        {
            startTor();
        });

        findViewById(R.id.stop_tor).setOnClickListener(v ->
        {
            stopTor();
        });

        try
        {
            /*
            bridge.getPdnsd()
                    .startDnsd(new PdnsdConfig()
                            .setBaseDir(dataDirectory)
                            .setUpstreamDnsAddress(dnsPort)
                            .setDnsServerAddress(new InetSocketAddress("127.0.0.1", 15053))
                    );

             */
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void startTor()
    {
        File filesDir = getFilesDir();
        File dataDirectory = new File(filesDir, "/transport");
        dataDirectory.mkdir();

        InetSocketAddress socksPort = InetSocketAddress.createUnresolved("127.0.0.1", 9050);
        InetSocketAddress controlPort = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
        InetSocketAddress dnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 9053);

        if (!TLJNIBridge.get().getTor().isTorRunning())
            torStatus.setText("Tor is starting");

        TLJNIBridge.get()
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
            TLJNIBridge.get()
                    .getTor().destroyTor();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        torStatus.setText("Tor is destroyed");
    }
}
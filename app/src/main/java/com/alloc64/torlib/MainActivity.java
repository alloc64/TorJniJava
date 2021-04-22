package com.alloc64.torlib;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.net.InetSocketAddress;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity
{
    static
    {
        System.loadLibrary("transport");
    }

    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TLJNIBridge bridge = new TLJNIBridge();

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(bridge.getTor().getTorVersion());

        try
        {
            File filesDir = getFilesDir();
            File dataDirectory = new File(filesDir, "/transport");
            dataDirectory.mkdir();

            InetSocketAddress socksPort = InetSocketAddress.createUnresolved("127.0.0.1", 9050);
            InetSocketAddress controlPort = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
            InetSocketAddress dnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 9053);

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

                                    checkIp();
                                }

                            });
                        }

                        @Override
                        public void onException(TorControlSocket socket, Exception e)
                        {
                            e.printStackTrace();
                        }
                    });

            this.httpClient = tor
                    .createOkHttpClient(socksPort)
                    .build();

            bridge.getPdnsd()
                    .startDnsd(new PdnsdConfig()
                    .setBaseDir(dataDirectory)
                    .setUpstreamDnsAddress(dnsPort)
                    .setDnsServerAddress(new InetSocketAddress("127.0.0.1", 15053))
            );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void checkIp()
    {
        try
        {
            Call call = httpClient.newCall(new Request.Builder()
                    .url("https://ipinfo.io/json")
                    .build());

            Response response = call.execute();

            if (response.isSuccessful())
                Log.i("IP test", response.body().string());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
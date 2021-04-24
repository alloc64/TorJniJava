package com.alloc64.torlib;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;

import java.io.File;
import java.net.InetSocketAddress;

public class BasicTorSampleActivity extends Activity
{
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        TLJNIBridge bridge = TLJNIBridge.get();

        try
        {
            File filesDir = getFilesDir();
            File dataDirectory = new File(filesDir, "/transport");
            dataDirectory.mkdir();

            InetSocketAddress socksPort = InetSocketAddress.createUnresolved("127.0.0.1", 9050);
            InetSocketAddress controlPort = InetSocketAddress.createUnresolved("127.0.0.1", 9051);
            InetSocketAddress dnsPort = InetSocketAddress.createUnresolved("127.0.0.1", 9053);

            TLJNIBridge.Tor tor = bridge.getTor();

            PasswordDigest controlPortPassword = PasswordDigest.generateDigest();

            tor.createTorConfig()
                    .setTorCommandLine(new TorConfig()
                            .addAllowMissingTorrc()
                            .setLog(TorConfig.LogSeverity.Notice, TorConfig.LogOutput.Syslog)
                            .setRunAsDaemon(false)
                            .setSocksPort(socksPort)
                            .setDnsPort(dnsPort)
                            .setSafeLogging("0")
                            .setControlPort(controlPort)
                            .setDataDirectory(dataDirectory)
                            .setHashedControlPassword(controlPortPassword))
                    .startTor()
                    .attachControlPort(controlPort, controlPortPassword, new TorControlSocket.TorEventHandler()
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
                    .startPdnsd(new PdnsdConfig()
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
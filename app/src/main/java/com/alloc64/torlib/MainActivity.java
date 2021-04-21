package com.alloc64.torlib;


import android.app.Activity;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.TextView;

import com.alloc64.jni.TLJNIBridge;

import com.alloc64.http.TorOkhttp3;
import com.alloc64.torlib.control.TorControlConnection;

import java.io.File;
import java.io.FileDescriptor;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity
{
    private TorControlConnection torControlConnection;

    static
    {
        System.loadLibrary("transport");
    }

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
            File torDataDirectory = new File(filesDir, "/transport");
            torDataDirectory.mkdir();

            TLJNIBridge.Tor tor = bridge.getTor();
            TLJNIBridge.Pdnsd pdnsd = bridge.getPdnsd();

            if(!tor.createTorConfig())
                throw new IllegalStateException("Unable to create transport config.");

            tor.setTorCommandLine(TorConfig.defaultConfig()
                    .setDataDirectory(torDataDirectory));

            ParcelFileDescriptor controlSocket = tor.setupTorControlSocket();

            this.torControlConnection = TorControlConnection.create(controlSocket, new TorControlConnection.EventHandler()
            {
                @Override
                public void circuitStatus(String status, String circID, String path)
                {

                }

                @Override
                public void streamStatus(String status, String streamID, String target)
                {

                }

                @Override
                public void orConnStatus(String status, String orName)
                {

                }

                @Override
                public void bandwidthUsed(long read, long written)
                {

                }

                @Override
                public void newDescriptors(List<String> orList)
                {

                }

                @Override
                public void message(String severity, String msg)
                {

                }

                @Override
                public void unrecognized(String type, String msg)
                {

                }
            });

            tor.startTor();

            torControlConnection.start();
            torControlConnection.sendAndWaitForResponse("OFF", null);

            Executors.newSingleThreadExecutor().execute(() ->
            {
                while (true)
                {
                    try
                    {
                        Thread.sleep(10_000);

                        OkHttpClient http = TorOkhttp3.create().build();

                        Call call = http.newCall(new Request.Builder()
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
            });

            //FileDescriptor vpnFileDescriptor = tor.prepareFileDescriptor("vpn_fd0");

            //pdnsd.startDnsd(new String[]{"", "-c", bridge.getPdnsd().createPdnsdConf(filesDir, "1.1.1.1", 53, "127.0.0.1", 9091).toString(), "-g", "-v2", "--nodaemon"});
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
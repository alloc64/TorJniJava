package org.zeroprism;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.zeroprism.http.TorOkhttp3;

import java.io.File;
import java.io.FileDescriptor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity
{
    static
    {
        System.loadLibrary("zeroprism");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JNIBridge bridge = new JNIBridge();

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(bridge.getTor().getTorVersion());

        try
        {
            File filesDir = getFilesDir();
            File torDataDirectory = new File(filesDir, "/transport");
            torDataDirectory.mkdir();

            JNIBridge.Tor tor = bridge.getTor();
            JNIBridge.Pdnsd pdnsd = bridge.getPdnsd();

            if(!tor.createTorConfig())
                throw new IllegalStateException("Unable to create transport config.");

            tor.setTorCommandLine(new String[] {
                    "tor",
                    "--allow-missing-torrc",
                    "--Log", "notice syslog",
                    "--SOCKSPort", "127.0.0.1:9050",
                    "--RunAsDaemon", "0",
                    "--DataDirectory", torDataDirectory.toString(),
                    "--SafeLogging", "0"
            });

            int controlSocket = tor.setupTorControlSocket();

            tor.startTor();

            Executors.newSingleThreadExecutor().execute(new Runnable()
            {
                @Override
                public void run()
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
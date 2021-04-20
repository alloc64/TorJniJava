package org.zeroprism;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;

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

        JNIBridge t = new JNIBridge();

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(t.getTor().getTorVersion());

        try
        {
            JNIBridge.Pdnsd pdnsd = t.getPdnsd();

            pdnsd.startDnsd(new String[]{"", "-c", pdnsd.createPdnsdConf(getFilesDir(), "1.1.1.1", 53, "127.0.0.1", 9091).toString(), "-g", "-v2", "--nodaemon"});
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
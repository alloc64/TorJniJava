package org.zeroprism;


import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

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

        Transport t = new Transport();

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(t.version());

        try
        {
            t.test(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
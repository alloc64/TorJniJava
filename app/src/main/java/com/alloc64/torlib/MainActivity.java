package com.alloc64.torlib;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;
import com.alloc64.vpn.VPNError;
import com.alloc64.vpn.client.AbstractVPNClient;
import com.alloc64.vpn.client.VPNClient;
import com.alloc64.vpn.messenger.IBasicMessage;

import java.io.File;
import java.net.InetSocketAddress;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity implements AbstractVPNClient.StateCallback
{
    static
    {
        System.loadLibrary("transport");
    }

    private VPNClient vpnClient = new VPNClient();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TLJNIBridge bridge = TLJNIBridge.get();

        TextView tv = findViewById(R.id.sample_text);
        tv.setText(bridge.getTor().getTorVersion());

        vpnClient.create(this);
        vpnClient.setStateCallback(this);
        vpnClient.rebind(this);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                vpnClient.connect(MainActivity.this, 0);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        vpnClient.destroy(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        vpnClient.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onMessageReceived(int messageType, IBasicMessage message)
    {

    }

    @Override
    public void onError(VPNError error)
    {

    }
}
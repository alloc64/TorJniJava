package com.alloc64.torlib;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.control.PasswordDigest;
import com.alloc64.torlib.control.TorControlSocket;
import com.alloc64.vpn.VPNConnectionState;
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
    private TextView statusTextView;
    private Button connectButton;

    private final VPNClient vpnClient = new VPNClient();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.statusTextView = findViewById(R.id.status);
        this.connectButton = findViewById(R.id.connect);

        vpnClient.create(this);
        vpnClient.setStateCallback(this);
        vpnClient.rebind(this);

        connectButton.setOnClickListener(this::onConnectButtonClicked);
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
        statusTextView.setText(String.format("Error: %s", error));
    }

    private void onConnectButtonClicked(View v)
    {
        VPNConnectionState state = vpnClient.getConnectionState();

        switch (state)
        {
            case Disconnected:
                statusTextView.setText("Tap connect to start.");
                connectButton.setText("Connect");
                break;

            case Connecting:
                statusTextView.setText("VPN is connecting.");
                connectButton.setText("Disconnect");
                break;

            case Connected:
                statusTextView.setText("VPN is connected.");
                connectButton.setText("Disconnect");
                break;
        }

        if(state == VPNConnectionState.Disconnected)
        {
            vpnClient.connect(this, 0);
        }
        else
        {
            vpnClient.disconnect();
        }
    }

}
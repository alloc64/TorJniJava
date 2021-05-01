package com.alloc64.torlib;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnError;
import com.alloc64.vpn.client.AbstractVpnClient;
import com.alloc64.vpn.client.VpnClient;
import com.alloc64.vpn.messenger.BasicMessage;

public class VpnTorActivity extends Activity implements AbstractVpnClient.StateCallback
{
    private TextView statusTextView;
    private Button connectButton;

    private final VpnClient vpnClient = new VpnClient();

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
    public void onMessageReceived(int messageType, BasicMessage message)
    {

    }

    @Override
    public void onError(VpnError error)
    {
        statusTextView.setText(String.format("Error: %s", error));
    }

    private void onConnectButtonClicked(View v)
    {
        VpnConnectionState state = vpnClient.getConnectionState();

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

        if(state == VpnConnectionState.Disconnected)
        {
            vpnClient.connect(this, 0);
        }
        else
        {
            vpnClient.disconnect();
        }
    }

}
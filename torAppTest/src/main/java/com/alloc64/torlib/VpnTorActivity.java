/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package com.alloc64.torlib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnError;
import com.alloc64.vpn.client.AbstractVpnClient;
import com.alloc64.vpn.client.TLVpnClient;
import com.alloc64.vpn.messenger.BasicMessage;
import com.alloc64.vpn.messenger.ConnectionRequestMessage;

public class VpnTorActivity extends Activity implements AbstractVpnClient.StateCallback
{
    private ProgressBar progressBar;
    private TextView statusTextView;
    private Button connectButton;

    private final TLVpnClient vpnClient = new TLVpnClient();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_tor);

        this.progressBar = findViewById(R.id.progress);
        this.statusTextView = findViewById(R.id.status);
        this.connectButton = findViewById(R.id.connect);

        vpnClient.create(this);
        vpnClient.setStateCallback(this);
        vpnClient.rebind(this);

        connectButton.setOnClickListener(this::onConnectButtonClicked);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        vpnClient.refreshState();
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
    public void onStateChanged(VpnConnectionState state)
    {
        switch (state)
        {
            case Disconnected:
                progressBar.setVisibility(View.INVISIBLE);
                statusTextView.setText("Tap connect to start.");
                connectButton.setText("Connect");
                break;

            case Connecting:
                progressBar.setVisibility(View.VISIBLE);
                statusTextView.setText("VPN is connecting.");
                connectButton.setText("Disconnect");
                break;

            case Connected:
                progressBar.setVisibility(View.INVISIBLE);
                statusTextView.setText("VPN is connected.");
                connectButton.setText("Disconnect");
                break;
        }
    }

    @Override
    public void onError(VpnError error)
    {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("An error occured: " + error)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                })
        .show();
    }

    private void onConnectButtonClicked(View v)
    {
        VpnConnectionState state = vpnClient.getConnectionState();

        if (state == VpnConnectionState.Disconnected)
        {
            vpnClient.connect(this, new ConnectionRequestMessage.Request("TorVPN", "DE"));
        }
        else
        {
            vpnClient.disconnect();
        }
    }
}
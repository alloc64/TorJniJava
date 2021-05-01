package com.alloc64.vpn.client;

import android.app.Activity;
import android.content.Context;

import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnMessageTypes;
import com.alloc64.vpn.messenger.ConnectionRequestMessage;

public class TLVpnClient extends AbstractVpnClient
{
    public void rebind(Context ctx)
    {
        rebind(ctx, com.alloc64.vpn.TLVpnService.class);
    }
}

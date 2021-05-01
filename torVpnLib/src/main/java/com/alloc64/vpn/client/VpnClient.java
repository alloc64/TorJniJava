package com.alloc64.vpn.client;

import android.app.Activity;
import android.content.Context;

import com.alloc64.vpn.TLVpnService;
import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnMessageTypes;
import com.alloc64.vpn.messenger.ProtoconfidConnectionStateMessage;

public class VpnClient extends AbstractVpnClient
{
    public void rebind(Context ctx)
    {
        rebind(ctx, TLVpnService.class);
    }

    @Override
    protected void onVPNPrepared(Activity activity, int protocolType)
    {
        super.onVPNPrepared(activity, protocolType);

        ProtoconfidConnectionStateMessage csm = new ProtoconfidConnectionStateMessage(VpnConnectionState.Connecting);

        //csm.setConnectionAddress(socketAddress);
        csm.setProtocolType(protocolType);
        //csm.setHandshakePayload(handshakePayload);

        sendMessage(VpnMessageTypes.ServiceStateChange, csm);
    }
}

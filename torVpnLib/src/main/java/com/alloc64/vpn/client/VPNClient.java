package com.alloc64.vpn.client;

import android.app.Activity;
import android.content.Context;

import com.alloc64.vpn.TLVpnService;
import com.alloc64.vpn.VPNConnectionState;
import com.alloc64.vpn.VPNMessageTypes;
import com.alloc64.vpn.messenger.ProtoconfidConnectionStateMessage;

public class VPNClient extends AbstractVPNClient
{
    public void rebind(Context ctx)
    {
        rebind(ctx, TLVpnService.class);
    }

    @Override
    protected void onVPNPrepared(Activity activity, int protocolType)
    {
        super.onVPNPrepared(activity, protocolType);

        ProtoconfidConnectionStateMessage csm = new ProtoconfidConnectionStateMessage(VPNConnectionState.Connecting);

        //csm.setConnectionAddress(socketAddress);
        csm.setProtocolType(protocolType);
        //csm.setHandshakePayload(handshakePayload);

        sendMessage(VPNMessageTypes.ServiceStateChange, csm);
    }
}

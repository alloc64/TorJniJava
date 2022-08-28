/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

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

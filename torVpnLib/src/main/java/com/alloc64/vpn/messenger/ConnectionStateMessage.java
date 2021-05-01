package com.alloc64.vpn.messenger;

import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnError;

public class ConnectionStateMessage extends BasicMessage<VpnConnectionState>
{
	private VpnError error;

	public ConnectionStateMessage(VpnConnectionState payload)
	{
		super(payload);
	}

	public VpnError getError()
	{
		return error;
	}

	public void setError(VpnError error)
	{
		this.error = error;
	}
}

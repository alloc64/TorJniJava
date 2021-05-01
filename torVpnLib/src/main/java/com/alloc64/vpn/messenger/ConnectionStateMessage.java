package com.alloc64.vpn.messenger;

import com.alloc64.vpn.VpnConnectionState;
import com.alloc64.vpn.VpnError;

public class ConnectionStateMessage extends BasicMessage<VpnConnectionState>
{
	private VpnError error;

	private int protocolType;

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

	public int getProtocolType()
	{
		return protocolType;
	}

	public void setProtocolType(int protocolType)
	{
		this.protocolType = protocolType;
	}
}

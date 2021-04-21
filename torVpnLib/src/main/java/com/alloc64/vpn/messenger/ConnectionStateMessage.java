package com.alloc64.vpn.messenger;

import com.alloc64.vpn.VPNConnectionState;
import com.alloc64.vpn.VPNError;

public class ConnectionStateMessage extends IBasicMessage<VPNConnectionState>
{
	private VPNError error;

	private int protocolType;

	public ConnectionStateMessage(VPNConnectionState payload)
	{
		super(payload);
	}

	public VPNError getError()
	{
		return error;
	}

	public void setError(VPNError error)
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

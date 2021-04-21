package com.alloc64.vpn.messenger;

import com.alloc64.vpn.VPNConnectionState;

import org.json.JSONObject;

public class ProtoconfidConnectionStateMessage extends ConnectionStateMessage
{
	private JSONObject handshakePayload;

	public ProtoconfidConnectionStateMessage(VPNConnectionState payload)
	{
		super(payload);
	}

	public JSONObject getHandshakePayload()
	{
		return handshakePayload;
	}

	public void setHandshakePayload(JSONObject handshakePayload)
	{
		this.handshakePayload = handshakePayload;
	}
}

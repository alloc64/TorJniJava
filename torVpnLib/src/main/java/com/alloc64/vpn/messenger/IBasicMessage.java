package com.alloc64.vpn.messenger;

public abstract class IBasicMessage<T>
{
	private final T payload;

	public IBasicMessage(T payload)
	{
		this.payload = payload;
	}

	public T getPayload()
	{
		return payload;
	}
}

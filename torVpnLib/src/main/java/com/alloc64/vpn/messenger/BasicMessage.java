package com.alloc64.vpn.messenger;

public abstract class BasicMessage<T>
{
	private final T payload;

	public BasicMessage(T payload)
	{
		this.payload = payload;
	}

	public T getPayload()
	{
		return payload;
	}
}

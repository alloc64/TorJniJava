/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

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

package com.alloc64.vpn.messenger;

import android.os.Parcel;
import android.os.Parcelable;

import com.alloc64.vpn.VpnConnectionState;

public class ConnectionRequestMessage extends ConnectionStateMessage
{
    public static class Request implements Parcelable
    {
        private final String sessionName;
        private final String countryIso;

        public Request(String sessionName, String countryIso)
        {
            this.sessionName = sessionName;
            this.countryIso = countryIso;
        }

        protected Request(Parcel in)
        {
            sessionName = in.readString();
            countryIso = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(sessionName);
            dest.writeString(countryIso);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        public static final Creator<Request> CREATOR = new Creator<Request>()
        {
            @Override
            public Request createFromParcel(Parcel in)
            {
                return new Request(in);
            }

            @Override
            public Request[] newArray(int size)
            {
                return new Request[size];
            }
        };

        public String getSessionName()
        {
            return sessionName;
        }

        public String getCountryIso()
        {
            return countryIso;
        }
    }

    private Request request;

    public ConnectionRequestMessage(VpnConnectionState payload, Request request)
    {
        super(payload);
        this.request = request;
    }

    public Request getRequest()
    {
        return request;
    }
}

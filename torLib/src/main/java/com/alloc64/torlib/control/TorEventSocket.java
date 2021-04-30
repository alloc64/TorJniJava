package com.alloc64.torlib.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.Response;

public class TorEventSocket extends TorAbstractControlSocket
{
    public interface EventHandler
    {
        void onEvent(TorEventSocket socket, List<Reply> replyList);
        void onException(TorEventSocket socket, Exception e);
    }

    private final List<String> registeredEvents;
    private final EventHandler eventHandler;

    public TorEventSocket(PasswordDigest password, List<String> registeredEvents, EventHandler eventHandler)
    {
        super(password);

        this.registeredEvents = registeredEvents;
        this.eventHandler = eventHandler;
    }

    @Override
    protected void onConnectedAsync()
    {
        super.onConnectedAsync();

        setEvents(registeredEvents);

        try
        {
            do
            {
                read();
            }
            while (socket.isConnected());
        }
        catch (Exception e)
        {
            onException(e);
        }
    }

    @Override
    protected void onReply(List<Reply> replyList)
    {
        super.onReply(replyList);

        List<Reply> eventReplies = new ArrayList<>();

        for(Reply r : replyList)
            if(r.getStatus() == ResponseCode.AsynchronousEventNotification.getValue())
                eventReplies.add(r);

        if(eventHandler != null && eventReplies.size() > 0)
            eventHandler.onEvent(TorEventSocket.this, eventReplies);
    }

    @Override
    protected void onException(Exception e)
    {
        super.onException(e);

        if(eventHandler != null)
            eventHandler.onException(TorEventSocket.this, e);
    }

    /**
     * Request that the server inform the client about interesting events.
     * Each element of <b>events</b> is one of the following Strings:
     * ["CIRC" | "STREAM" | "ORCONN" | "BW" | "DEBUG" |
     *  "INFO" | "NOTICE" | "WARN" | "ERR" | "NEWDESC" | "ADDRMAP"] .
     *
     * Any events not listed in the <b>events</b> are turned off; thus, calling
     * setEvents with an empty <b>events</b> argument turns off all event reporting.
     * @return
     */
    public List<Reply> setEvents(List<String> events)
    {
        StringBuilder sb = new StringBuilder("SETEVENTS");
        for (String event : events)
            sb.append(" ").append(event);

        sb.append("\r\n");

        return send(sb.toString(), null);
    }
}

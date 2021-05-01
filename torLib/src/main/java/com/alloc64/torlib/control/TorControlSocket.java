package com.alloc64.torlib.control;

import com.alloc64.jni.TLJNIBridge;
import com.alloc64.torlib.TorConfig;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * See https://gitweb.torproject.org/torspec.git/tree/control-spec.txt
 */
public class TorControlSocket extends TorAbstractControlSocket
{
    private ConnectionHandler eventHandler;

    protected TorControlSocket(PasswordDigest password, MainThreadDispatcher mainThreadDispatcher)
    {
        super(password, mainThreadDispatcher);
    }

    public TorControlSocket(PasswordDigest password, ConnectionHandler eventHandler, MainThreadDispatcher mainThreadDispatcher)
    {
        this(password, mainThreadDispatcher);
        this.setEventHandler(eventHandler);
    }

    public void setEventHandler(ConnectionHandler eventHandler)
    {
        this.eventHandler = eventHandler;
    }

    @Override
    protected void onConnectedAsync()
    {
        super.onConnectedAsync();

        if(eventHandler != null)
            eventHandler.onConnectedAsync(this);

        getMainThreadDispatcher().dispatch(() ->
        {
            if(eventHandler != null)
                eventHandler.onConnected(TorControlSocket.this);
        });
    }

    @Override
    protected void onException(Exception e)
    {
        super.onException(e);

        if(eventHandler != null)
            eventHandler.onException(TorControlSocket.this, e);
    }

    @Override
    protected void onReply(List<Reply> replyList)
    {
        super.onReply(replyList);

        if(eventHandler != null)
            eventHandler.onReply(this, replyList);
    }

    /**
     * Sends a signal from the controller to the Tor server.
     * <b>signal</b> is one of the following Strings:
     * <ul>
     * <li>"RELOAD" or "HUP" :  Reload config items, refetch directory</li>
     * <li>"SHUTDOWN" or "INT" : Controlled shutdown: if server is an OP, exit immediately.
     *     If it's an OR, close listeners and exit after 30 seconds</li>
     * <li>"DUMP" or "USR1" : Dump stats: log information about open connections and circuits</li>
     * <li>"DEBUG" or "USR2" : Debug: switch all open logs to loglevel debug</li>
     * <li>"HALT" or "TERM" : Immediate shutdown: clean up and exit now</li>
     * </ul>
     */
    public Reply signal(Signal signal)
    {
        return defaultOrNull(
                send(String.format("SIGNAL %s\r\n", signal))
        );
    }

    /**
     * Queries the Tor server for keyed values that are not stored in the torrc
     * configuration file.  Returns a map of keys to values.
     * <p>
     * Recognized keys include:
     * <ul>
     * <li>"version" : The version of the server's software, including the name
     *  of the software. (example: "Tor 0.0.9.4")</li>
     * <li>"desc/id/<OR identity>" or "desc/name/<OR nickname>" : the latest server
     * descriptor for a given OR, NUL-terminated.  If no such OR is known, the
     * corresponding value is an empty string.</li>
     * <li>"network-status" : a space-separated list of all known OR identities.
     * This is in the same format as the router-status line in directories;
     * see tor-spec.txt for details.</li>
     * <li>"addr-mappings/all"</li>
     * <li>"addr-mappings/config"</li>
     * <li>"addr-mappings/cache"</li>
     * <li>"addr-mappings/control" : a space-separated list of address mappings, each
     * in the form of "from-address=to-address".  The 'config' key
     * returns those address mappings set in the configuration; the 'cache'
     * key returns the mappings in the client-side DNS cache; the 'control'
     * key returns the mappings set via the control interface; the 'all'
     * target returns the mappings set through any mechanism.</li>
     * <li>"circuit-status" : A series of lines as for a circuit status event. Each line is of the form:
     * "CircuitID CircStatus Path"</li>
     * <li>"stream-status" : A series of lines as for a stream status event.  Each is of the form:
     * "StreamID StreamStatus CircID Target"</li>
     * <li>"orconn-status" : A series of lines as for an OR connection status event.  Each is of the
     * form: "ServerID ORStatus"</li>
     * </ul>
     *
     * @return
     */
    public Map<String, String> getInfo(List<String> keys)
    {
        StringBuilder sb = new StringBuilder("GETINFO");
        for (String key : keys)
            sb.append(" ").append(key);

        sb.append("\r\n");

        List<Reply> replyList = send(sb.toString());

        Map<String, String> m = new LinkedHashMap<>();

        for (Reply reply : replyList)
        {
            String msg = reply.getMessage();

            int idx = msg.indexOf('=');
            if (idx < 0)
                break;
            String k = msg.substring(0, idx);

            String v;

            if (reply.getRest() != null)
                v = reply.getRest();
            else
                v = msg.substring(idx + 1);

            m.put(k, v);
        }

        return m;
    }

    public String getInfo(String key)
    {
        return getInfo(Collections.singletonList(key)).get(key);
    }

    /**
     * Change the value of the configuration option 'key' to 'val'.
     */
    public void setConf(String key, String value)
    {
        List<String> lst = new ArrayList<>();
        lst.add(key + " " + value);
        setConf(lst);
    }

    /**
     * Change the values of the configuration options stored in kvMap.
     */
    public void setConf(Map<String, String> kvMap)
    {
        List<String> lst = new ArrayList<>();

        for (Map.Entry<String, String> ent : kvMap.entrySet())
            lst.add(ent.getKey() + " " + ent.getValue() + "\n");

        setConf(lst);
    }

    /**
     * Changes the values of the configuration options stored in
     * <b>kvList</b>.  Each list element in <b>kvList</b> is expected to be
     * String of the format "key value".
     * <p>
     * Tor behaves as though it had just read each of the key-value pairs
     * from its configuration file.  Keywords with no corresponding values have
     * their configuration values reset to their defaults.  setConf is
     * all-or-nothing: if there is an error in any of the configuration settings,
     * Tor sets none of them.
     * <p>
     * When a configuration option takes multiple values, or when multiple
     * configuration keys form a context-sensitive group (see getConf below), then
     * setting any of the options in a setConf command is taken to reset all of
     * the others.  For example, if two ORBindAddress values are configured, and a
     * command arrives containing a single ORBindAddress value, the new
     * command's value replaces the two old values.
     * <p>
     * To remove all settings for a given option entirely (and go back to its
     * default value), include a String in <b>kvList</b> containing the key and no value.
     *
     * @return
     */
    public List<Reply> setConf(Collection<String> kvList)
    {
        StringBuilder b = new StringBuilder("SETCONF");

        for (String kv : kvList)
        {
            int i = kv.indexOf(' ');

            if (i == -1)
                b.append(" ").append(kv);

            b.append(" ")
                    .append(kv.substring(0, i))
                    .append("=")
                    .append(quote(kv.substring(i + 1)));
        }

        b.append("\r\n");

        return send(b.toString());
    }

    /**
     * Try to reset the values listed in the collection 'keys' to their
     * default values.
     *
     * @return
     */
    public List<Reply> resetConf(Collection<String> keys)
    {
        StringBuilder b = new StringBuilder("RESETCONF");
        for (String key : keys)
            b.append(" ").append(key);

        b.append("\r\n");
        return send(b.toString());
    }

    /**
     * Return the value of the configuration option 'key'
     */
    public List<Config> getConf(String key) throws IOException
    {
        List<String> lst = new ArrayList<>();
        lst.add(key);
        return getConf(lst);
    }

    /**
     * Requests the values of the configuration variables listed in <b>keys</b>.
     * Results are returned as a list of ConfigEntry objects.
     * <p>
     * If an option appears multiple times in the configuration, all of its
     * key-value pairs are returned in order.
     * <p>
     * Some options are context-sensitive, and depend on other options with
     * different keywords.  These cannot be fetched directly.  Currently there
     * is only one such option: clients should use the "HiddenServiceOptions"
     * virtual keyword to get all HiddenServiceDir, HiddenServicePort,
     * HiddenServiceNodes, and HiddenServiceExcludeNodes option settings.
     */
    public List<Config> getConf(Collection<String> keys) throws IOException
    {
        StringBuilder sb = new StringBuilder("GETCONF");
        for (String key : keys)
            sb.append(" ").append(key);

        sb.append("\r\n");
        List<Reply> lst = send(sb.toString(), null);
        List<Config> result = new ArrayList<Config>();

        for (Reply reply : lst)
        {
            String kv = reply.getMessage();
            int idx = kv.indexOf('=');

            if (idx >= 0)
                result.add(new Config(kv.substring(0, idx), kv.substring(idx + 1), false));
            else
                result.add(new Config(kv, "", true));
        }
        return result;
    }


    public void setNetworkEnabled(boolean isEnabled)
    {
        setConf(TorConfig.DISABLE_NETWORK, isEnabled ? "0" : "1");
    }

    public void reloadTorNetwork()
    {
        setNetworkEnabled(true);
        setNetworkEnabled(false);
    }
    
    /**
     * Set targeting by country code, or by exit node ID.
     *
     * GEO IP files must be set in case you are targeting with country codes.
     *
     * @param exitNodeTargeting
     */
    public void setExitNodeTargeting(List<String> exitNodeTargeting)
    {
        setConf(TorConfig.EXIT_NODES, String.format("{%s}", StringUtils.join( exitNodeTargeting, ",")));
        setConf(TorConfig.STRICT_NODES, "1");

        reloadTorNetwork();
    }

    public void disableExitNodeTargeting()
    {
        resetConf(Arrays.asList(TorConfig.EXIT_NODES, TorConfig.STRICT_NODES));
        reloadTorNetwork();
    }
}

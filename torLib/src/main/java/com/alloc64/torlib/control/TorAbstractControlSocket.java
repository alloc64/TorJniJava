package com.alloc64.torlib.control;

import com.alloc64.jni.TLJNIBridge;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * See https://gitweb.torproject.org/torspec.git/tree/control-spec.txt
 */
public class TorAbstractControlSocket implements Runnable
{
    public enum ResponseCode
    {
        OK(250),
        OperationWasUnnecessary(251), // [Tor has declined to perform the operation, but no harm was done.]
        ResourceExhausted(451),
        SyntaxErrorProtocol(500),
        UnrecognizedCommand(510),
        UnimplementedCommand(511),
        SyntaxErrorInCommandArgument(512),
        UnrecognizedCommandArgument(513),
        AuthenticationRequired(514),
        BadAuthentication(515),
        UnspecifiedTorError(550),
        InternalError(551), // [Something went wrong inside Tor, so that the client's request couldn't be fulfilled.]
        UnrecognizedEntity(552), // [A configuration key, a stream ID, circuit ID, event, mentioned in the command did not actually exist.]
        InvalidConfigurationValue(553),
        InvalidDescriptor(554),
        UnmanagedEntity(555),
        AsynchronousEventNotification(650);

        private int val;

        ResponseCode(int val)
        {
            this.val = val;
        }

        public int getValue()
        {
            return val;
        }
    }

    public enum Signal
    {
        RELOAD("RELOAD"), // Reload: reload config items.
        SHUTDOWN("SHUTDOWN"), // Controlled shutdown: if server is an OP, exit immediately. If it's an OR, close listeners and exit after ShutdownWaitLength seconds.
        DUMP("DUMP"), // Dump stats: log information about open connections and circuits.
        DEBUG("DEBUG"), // Debug: switch all open logs to loglevel debug.
        HALT("HALT"), // Immediate shutdown: clean up and exit now.
        CLEARDNSCACHE("CLEARDNSCACHE"), // Forget the client-side cached IPs for all hostnames.
        NEWNYM("NEWNYM"), // Switch to clean circuits, so new application requests don't share any circuits with old ones.  Also clears the client-side DNS cache.  (Tor MAY rate-limit its response to this signal.)
        HEARTBEAT("HEARTBEAT"), // Make Tor dump an unscheduled Heartbeat message to log.
        DORMANT("DORMANT"), // Tell Tor to become "dormant".  A dormant Tor will try to avoid CPU and network usage until it receives user-initiated network request.  (Don't use this on relays or hidden services yet!)
        ACTIVE("ACTIVE"); // Tell Tor to stop being "dormant", as if it had received a user-initiated network request.

        private String val;

        Signal(String val)
        {
            this.val = val;
        }

        public String getValue()
        {
            return val;
        }
    }

    public abstract static class ConnectionHandler
    {
        /**
         * Dispatched on connect thread
         *
         * @param socket
         */
        public void onConnectedAsync(TorControlSocket socket)
        {

        }

        /**
         * Dispatched on main thread
         *
         * @param socket
         */
        public void onConnected(TorControlSocket socket)
        {

        }

        public void onReply(TorControlSocket socket, List<Reply> replyList)
        {

        }

        public void onException(TorControlSocket socket, Exception e)
        {

        }
    }

    public interface Callback
    {
        void onResult(List<Reply> replyList);
    }

    public interface InfoCallback
    {
        void onResult(TorControlSocket socket, Map<String, String> result);
    }

    public interface MainThreadDispatcher
    {
        void dispatch(Runnable runnable);
    }

    public static class Reply
    {
        private int status;
        private String message;
        private String rest;

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public String getRest()
        {
            return rest;
        }

        public void setRest(String rest)
        {
            this.rest = rest;
        }

        public void setStatus(String val)
        {
            try
            {
                this.setStatus(Integer.parseInt(val));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static class Config
    {
        private final String key;
        private final String value;
        private final boolean isDefault;

        public Config(String key, String value, boolean isDefault)
        {
            this.key = key;
            this.value = value;
            this.isDefault = isDefault;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }

        public boolean isDefault()
        {
            return isDefault;
        }
    }

    private static final int CONNECTION_RETRY_COUNT = 20;

    private final PasswordDigest password;
    private final MainThreadDispatcher mainThreadDispatcher;
    private InetSocketAddress socketAddress;

    protected Socket socket;

    private BufferedReader inputStream;
    private OutputStreamWriter outputStream;

    private final Executor asyncSendExecutor = Executors.newSingleThreadExecutor();

    protected TorAbstractControlSocket(PasswordDigest password, MainThreadDispatcher mainThreadDispatcher)
    {
        this.password = password;
        this.mainThreadDispatcher = mainThreadDispatcher;
    }

    public MainThreadDispatcher getMainThreadDispatcher()
    {
        return mainThreadDispatcher;
    }

    public void connect(InetSocketAddress socketAddress)
    {
        this.socketAddress = socketAddress;

        Executors.newSingleThreadExecutor().execute(this);
    }

    @Override
    public void run()
    {
        try
        {
            Thread.sleep(1000); // sleep in case TOR is not ready

            boolean alreadyConnected = false;

            for (int i = 0; i < CONNECTION_RETRY_COUNT && !alreadyConnected; i++)
            {
                try
                {
                    this.socket = new Socket();

                    if (socketAddress.isUnresolved())
                        this.socketAddress = new InetSocketAddress(socketAddress.getHostName(), socketAddress.getPort());

                    socket.connect(socketAddress);
                    alreadyConnected = true;

                    this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    this.outputStream = new OutputStreamWriter(socket.getOutputStream());

                    Reply authenticationReply = authenticate(password);

                    if (authenticationReply == null || authenticationReply.getStatus() != ResponseCode.OK.getValue())
                        throw new IllegalStateException("Authentication failed");

                    onConnectedAsync();
                }
                catch (IOException e)
                {
                    if (alreadyConnected)
                        throw e;

                    onException(e);
                    Thread.sleep(i * 1000);
                }
            }
        }
        catch (Exception e)
        {
            onException(e);

            try
            {
                close();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }

    protected void onConnectedAsync()
    {

    }

    protected void onReply(List<Reply> replyList)
    {
    }

    protected void onException(Exception e)
    {
        e.printStackTrace();
    }

    protected List<Reply> read() throws IOException
    {
        List<Reply> replyList = new ArrayList<>();

        char c;

        do
        {
            String line = inputStream.readLine();

            if (line == null)
            {

                if (replyList.isEmpty())
                    return replyList;

                throw new IOException("Connection closed.");
            }

            if (line.length() < 4)
                throw new IllegalStateException("Line (\"" + line + "\") too short");

            Reply reply = new Reply();

            reply.setStatus(line.substring(0, 3));

            c = line.charAt(3);
            reply.setMessage(line.substring(4));
            String rest = null;

            if (c == '+')
            {
                StringBuilder data = new StringBuilder();

                while (true)
                {
                    line = inputStream.readLine();

                    if (line.equals("."))
                        break;
                    else if (line.startsWith("."))
                        line = line.substring(1);
                    data.append(line).append('\n');
                }

                rest = data.toString();
            }

            reply.setRest(rest);

            int status = reply.getStatus();

            if (status == ResponseCode.AuthenticationRequired.getValue() || status == ResponseCode.BadAuthentication.getValue())
                throw new IllegalStateException(reply.getMessage());

            replyList.add(reply);
        }
        while (c != ' ');

        onReply(replyList);

        return replyList;
    }

    public void close() throws IOException
    {
        if (socket != null)
            socket.close();
    }

    public List<Reply> send(String command)
    {
        return send(command, null);
    }

    public List<Reply> send(String command, String params)
    {
        if (outputStream == null)
            return null;

        try
        {
            outputStream.write(command);

            if (!StringUtils.isEmpty(params))
            {
                StringTokenizer st = new StringTokenizer(params, "\n");
                while (st.hasMoreTokens())
                {
                    String line = st.nextToken();

                    if (line.startsWith("."))
                        line = "." + line;
                    if (line.endsWith("\r"))
                        line += "\n";
                    else
                        line += "\r\n";

                    outputStream.write(line);
                }

                outputStream.write(".\r\n");
            }

            outputStream.flush();

            return read();
        }
        catch (Exception e)
        {
            onException(e);
        }
        return null;
    }

    public void sendAsync(String command, String params, Callback callback)
    {
        asyncSendExecutor.execute(() ->
        {
            try
            {
                List<Reply> reply = send(command, params);

                getMainThreadDispatcher().dispatch(() ->
                {
                    if (reply != null && callback != null)
                        callback.onResult(reply);
                });
            }
            catch (Exception e)
            {
                onException(e);
            }
        });
    }

    public void sendAsync(String command, Callback callback)
    {
        sendAsync(command, null, callback);
    }

    /**
     * Authenticates the controller to the Tor server.
     * <p>
     * By default, the current Tor implementation trusts all local users, and
     * the controller can authenticate itself by calling authenticate(new byte[0]).
     * <p>
     * If the 'CookieAuthentication' option is true, Tor writes a "magic cookie"
     * file named "control_auth_cookie" into its data directory.  To authenticate,
     * the controller must send the contents of this file in <b>auth</b>.
     * <p>
     * If the 'HashedControlPassword' option is set, <b>auth</b> must contain the salted
     * hash of a secret password.  The salted hash is computed according to the
     * S2K algorithm in RFC 2440 (OpenPGP), and prefixed with the s2k specifier.
     * This is then encoded in hexadecimal, prefixed by the indicator sequence
     * "16:".
     * <p>
     * You can generate the salt of a password by calling
     * 'tor --hash-password <password>'
     * or by using the provided PasswordDigest class.
     * To authenticate under this scheme, the controller sends Tor the original
     * secret that was used to generate the password.
     */
    private Reply authenticate(PasswordDigest password)
    {
        return defaultOrNull(
                send(String.format("AUTHENTICATE %s\r\n", password.getSecretHex()), null)
        );
    }

    public <T> T defaultOrNull(List<T> list)
    {
        if (list != null && list.size() > 0)
            return list.get(0);

        return null;
    }

    protected String quote(String s)
    {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '\r':
                case '\n':
                case '\\':
                case '\"':
                    sb.append('\\');
            }

            sb.append(c);
        }

        sb.append('\"');
        return sb.toString();
    }
}

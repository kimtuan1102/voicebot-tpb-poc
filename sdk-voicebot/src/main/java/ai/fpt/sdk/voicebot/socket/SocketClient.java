package ai.fpt.sdk.voicebot.socket;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import ai.fpt.sdk.voicebot.callback.SocketEventListener;
import io.github.sac.Ack;
import io.github.sac.BasicListener;
import io.github.sac.Emitter;
import io.github.sac.ReconnectStrategy;
import io.github.sac.Socket;

public class SocketClient {
    private String socketEndpoint;
    private static SocketClient socketClient = null;
    private static final String AUTHENTICATION_EVENT = "sender_request_authenticate";
    private static final String USER_SENT_MESSAGE_EVENT = "user_send_message";
    public static String TAG = "Socket Client";
    private SocketAuthenticateData socketAuthenticateData;
    private Socket socket;
    private SocketEventListener socketEventListener;

    private void setSocketEventListener(SocketEventListener socketEventListener) {
        this.socketEventListener = socketEventListener;
    }

    private void setSocketEndpoint(String socketEndpoint) {
        this.socketEndpoint = socketEndpoint;
    }

    public static SocketClient create(String socketEndpoint, SocketEventListener socketEventListener) {
        if (socketClient == null) {
            socketClient = new SocketClient();
            socketClient.setSocketEndpoint(socketEndpoint);
            socketClient.setSocketEventListener(socketEventListener);
        }
        return socketClient;
    }

    public void connect(SocketAuthenticateData socketAuthenticateData) {
        this.socketAuthenticateData = socketAuthenticateData;
        socket = new Socket(this.socketEndpoint);
        socket.setListener(new BasicListener() {
            @Override
            public void onConnected(Socket socket, Map<String, List<String>> headers) {
                sendAuthenticate(socketAuthenticateData);
                Log.i(TAG, "Connected to endpoint");
            }

            @Override
            public void onDisconnected(Socket socket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                Log.i(TAG, "Socket is disconnected");
            }

            @Override
            public void onConnectError(Socket socket, WebSocketException exception) {
                Log.e(TAG, "Got connect error " + exception);
            }

            @Override
            public void onAuthentication(Socket socket, Boolean status) {
                if (status) {
                    Log.i(TAG, "Socket is authenticated");
                } else {
                    Log.i(TAG, "Authentication is required (optional)");
                }
            }

            @Override
            public void onSetAuthToken(String token, Socket socket) {
                socket.setAuthToken(token);
            }
        });
        socket.setReconnection(new ReconnectStrategy().setMaxAttempts(10).setDelay(3000));
        socket.connectAsync();
    }

    public void disconnect() {
        socket.disconnect();
    }

    private void sendAuthenticate(SocketAuthenticateData authenticateData) {
        String str = authenticateData.getClientID() + "." + authenticateData.getBotCode();
        String subscribeChannel = new String(Hex.encodeHex(DigestUtils.md5(str))).concat("@").concat(authenticateData.getBotCode()).concat("/livechat");
        JSONObject object = new JSONObject();
        try {
            object.put("sender_id", authenticateData.getClientID());
            object.put("token", authenticateData.getClientToken());
            object.put("sender_name", authenticateData.getClientName());
            object.put("bot_code", authenticateData.getBotCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit(AUTHENTICATION_EVENT, object, new Ack() {
            @Override
            public void call(String name, Object error, Object data) {
                if (error == null) {
                    socket.createChannel(subscribeChannel).subscribe((name1, error1, data1) -> {
                        if (error1 == null) {
                            Log.i(TAG, "Subscribed to channel " + name1);
                            socket.onSubscribe(subscribeChannel, new Emitter.Listener() {
                                @Override
                                public void call(String name, Object data) {
                                    socketEventListener.onMessage(data);
                                }
                            });
                        }
                    });
                } else Log.e(TAG, error.toString());
            }
        });
    }

    public void sendTextMessage(String text) {
        JSONObject object=new JSONObject();
        JSONObject childData = new JSONObject();
        try {
            object.put("channel","livechat");
            object.put("sub_channel","subchannel");
            object.put("sender_id", socketAuthenticateData.getClientID());
            object.put("sender_name",socketAuthenticateData.getClientName());
            object.put("bot_code", socketAuthenticateData.getBotCode());
            childData.put("type", "text");
            childData.put("content", text);
            object.put("message", childData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit(USER_SENT_MESSAGE_EVENT, object);
    }
}

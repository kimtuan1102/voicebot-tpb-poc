package ai.fpt.sdk.voicebot;

import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import ai.fpt.sdk.voicebot.callback.MessageListener;
import ai.fpt.sdk.voicebot.callback.RecognizeListener;
import ai.fpt.sdk.voicebot.callback.SocketEventListener;
import ai.fpt.sdk.voicebot.socket.SocketAuthenticateData;
import ai.fpt.sdk.voicebot.socket.SocketClient;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoiceBotClient implements SocketEventListener {
    private static VoiceBotClient voiceBotClient = null;
    private static MediaRecorder mRecorder;
    private static SocketClient socketClient;
    private final RecognizeListener recognizeListener;
    private final MessageListener messageListener;
    private final VoiceBotSettings voiceBotSettings;
    public static final String TAG = "Voice Bot Client";
    private VoiceBotClient(VoiceBotSettings voiceBotSettings, RecognizeListener recognizeListener, MessageListener messageListener) {
        this.recognizeListener = recognizeListener;
        this.voiceBotSettings = voiceBotSettings;
        this.messageListener = messageListener;
    }

    public static VoiceBotClient create(VoiceBotSettings voiceBotSettings, RecognizeListener recognizeListener, MessageListener messageListener) {
        if (voiceBotClient == null) {
            voiceBotClient = new VoiceBotClient(voiceBotSettings, recognizeListener, messageListener);
        }
        return voiceBotClient;
    }
    public void startRecording() {
        String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.wav";
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mRecorder.setOutputFile(mFileName);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        mRecorder.start();
    }
    public File stopRecording() {
        try {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        } catch(RuntimeException stopException) {
            Log.e("Exception", "Runtime Exception");
        }
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.wav");
    }
    public void speechToText(File audio){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String voiceAPIEndPoint = voiceBotSettings.getVoiceAPIEndpoint();
                    String voiceAPIKey = voiceBotSettings.getVoiceAPIKey();
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), audio);
                    Request request = new Request.Builder()
                            .url(voiceAPIEndPoint)
                            .addHeader("api_key", voiceAPIKey)
                            .post(requestBody)
                            .build();
                    Call call = client.newCall(request);
                    Response response = call.execute();
                    Log.d("app", response.toString());
                    String json = response.body().string();
                    Log.d("app", json);
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray hypotheses = jsonObject.getJSONArray("hypotheses");
                    String utterance = hypotheses.getJSONObject(0).getString("utterance");
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run() {
                            recognizeListener.onRecognizedSuccess(utterance);
                        }
                    });
                }
                catch (JSONException | IOException exception) {
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run() {
                            recognizeListener.onRecognizedFailed();
                        }
                    });
                }
            }
        });
        thread.start();
    }
    public void connectSocket() {
        socketClient = SocketClient.create(voiceBotSettings.getSocketEndpoint(), this);
        SocketAuthenticateData socketAuthenticateData = new SocketAuthenticateData();
        socketAuthenticateData.setBotCode(voiceBotSettings.getBotCode());
        socketAuthenticateData.setClientID(UUID.randomUUID().toString());
        socketAuthenticateData.setClientName(UUID.randomUUID().toString());
        socketAuthenticateData.setClientToken("");
        socketClient.connect(socketAuthenticateData);
    }
    public void disconnectSocket() {
        socketClient.disconnect();
    }
    public void sendMessageToSocket(String message) {
        socketClient.sendTextMessage(message);
    }
    @Override
    public void onMessage(Object data) {
        JSONObject object= (JSONObject) data;
        try {
            String messageTxt = object.getJSONObject("content").getString("text");
            if (object.getInt("source") == 1) {
                messageListener.onUserMessage(messageTxt);
            }
            else if (object.getInt("source") == 2) {
                if (messageTxt.startsWith("#cmd#")) {
                    String deepLink = messageTxt.substring(5);
                    messageListener.onCommand(deepLink);
                }
                else {
                    messageListener.onBotMessage(messageTxt);
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "Message not have content field");
        }
    }
}

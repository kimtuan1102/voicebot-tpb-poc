package ai.fpt.voicebot;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;


import ai.fpt.sdk.voicebot.VoiceBotClient;
import ai.fpt.sdk.voicebot.VoiceBotSettings;
import ai.fpt.sdk.voicebot.callback.MessageListener;
import ai.fpt.sdk.voicebot.callback.RecognizeListener;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements RecognizeListener, MessageListener {
    public static final String TAG = "FPT-AI-VoiceBot";
    private ImageButton startRecording, stopRecording;
    private ProgressBar loading;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private VoiceBotSettings voiceBotSettings;
    private VoiceBotClient voiceBotClient;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startRecording = (ImageButton) findViewById(R.id.startRecording);
        stopRecording = (ImageButton) findViewById(R.id.stopRecording);
        chatContainer = (LinearLayout) findViewById(R.id.chatContainer);
        scrollView = (ScrollView) findViewById(R.id.scroll);
        loading = (ProgressBar)findViewById(R.id.loading);
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            voiceBotSettings = VoiceBotSettings.buildSettings(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        voiceBotClient = VoiceBotClient.create(voiceBotSettings, this, this);
        voiceBotClient.connectSocket();
        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voiceBotClient.disconnectSocket();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (permissionToRecord && permissionToStore) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean checkPermissions() {
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
    private void startRecording() {
        if (checkPermissions()) {
            startRecording.setVisibility(View.INVISIBLE);
            stopRecording.setVisibility(View.VISIBLE);
            voiceBotClient.startRecording();
        }
        else {
            requestPermissions();
        }
    }
    private void stopRecording() {
        startRecording.setVisibility(View.INVISIBLE);
        stopRecording.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.VISIBLE);
        File audio = voiceBotClient.stopRecording();
        voiceBotClient.speechToText(audio);
    }
    private TextView getTextView(){
        TextView textView = new TextView(MainActivity.this);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 5);
        textView.setLayoutParams(params);
        return textView;
    }
    @Override
    public void onUserMessage(String message) {
        final TextView textView=getTextView();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(10,10,10,10);
        params.gravity = Gravity.LEFT;
        textView.setLayoutParams(params);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(25, 25,25,25);
        textView.setBackgroundResource(R.drawable.user_message);
        textView.setText(Html.fromHtml(message));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatContainer.addView(textView);
                scrollView.scrollTo(0,scrollView.getBottom());
            }
        });
    }

    @Override
    public void onBotMessage(String message) {
        final TextView textView=getTextView();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(10,10,10,10);
        params.gravity = Gravity.RIGHT;
        textView.setLayoutParams(params);
        textView.setTextColor(Color.WHITE);
        textView.setPadding(25, 25,25,25);
        textView.setBackgroundResource(R.drawable.bot_message);
        textView.setText(Html.fromHtml(message));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatContainer.addView(textView);
                scrollView.scrollTo(0,scrollView.getBottom());
            }
        });
    }
    @Override
    public void onCommand(String deepLink) {
        //        TODO Handle Command
        Log.e(TAG, "Command");
    }
    @Override
    public void onRecognizedSuccess(String utterance) {
        loading.setVisibility(View.INVISIBLE);
        startRecording.setVisibility(View.VISIBLE);
        stopRecording.setVisibility(View.INVISIBLE);
        voiceBotClient.sendMessageToSocket(utterance);
        Log.d(TAG, utterance);
    }

    @Override
    public void onRecognizedFailed() {
        loading.setVisibility(View.INVISIBLE);
        startRecording.setVisibility(View.VISIBLE);
        stopRecording.setVisibility(View.INVISIBLE);
        Log.e(TAG, "Recognize Failed");
    }
}
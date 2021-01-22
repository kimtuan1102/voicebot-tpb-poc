package ai.fpt.sdk.voicebot.callback;

public interface RecognizeListener {
    void onRecognizedSuccess(String utterance);
    void onRecognizedFailed();
}

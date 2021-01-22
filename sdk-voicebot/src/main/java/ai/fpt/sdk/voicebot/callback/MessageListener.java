package ai.fpt.sdk.voicebot.callback;

public interface MessageListener {
    void onUserMessage(String message);
    void onBotMessage(String message);
    void onCommand(String deepLink);
}

package ai.fpt.sdk.voicebot;

import android.content.pm.ApplicationInfo;

public class VoiceBotSettings {
    private static VoiceBotSettings voiceBotClientSettings = null;

    private String voiceAPIKey;
    private String voiceAPIEndpoint;
    private String socketEndpoint;
    private String botCode;

    public String getVoiceAPIKey() {
        return voiceAPIKey;
    }

    public void setVoiceAPIKey(String voiceAPIKey) {
        this.voiceAPIKey = voiceAPIKey;
    }

    public String getVoiceAPIEndpoint() {
        return voiceAPIEndpoint;
    }

    public void setVoiceAPIEndpoint(String voiceAPIEndpoint) {
        this.voiceAPIEndpoint = voiceAPIEndpoint;
    }

    public String getSocketEndpoint() {
        return socketEndpoint;
    }

    public void setSocketEndpoint(String socketEndpoint) {
        this.socketEndpoint = socketEndpoint;
    }

    public String getBotCode() {
        return botCode;
    }

    public void setBotCode(String botCode) {
        this.botCode = botCode;
    }

    public static VoiceBotSettings buildSettings(ApplicationInfo applicationInfo) {
        if (voiceBotClientSettings == null) {
            voiceBotClientSettings = new VoiceBotSettings();
            String _voiceAPIKey = applicationInfo.metaData.getString("ai.fpt.voicebot.API_KEY");
            String _voiceAPIEndPoint = applicationInfo.metaData.getString("ai.fpt.voicebot.API_ENDPOINT");
            String _socketEndpoint = applicationInfo.metaData.getString("ai.fpt.voicebot.SOCKET_ENDPOINT");
            String _botCode = applicationInfo.metaData.getString("ai.fpt.voicebot.BOT_CODE");
            voiceBotClientSettings.setVoiceAPIKey(_voiceAPIKey);
            voiceBotClientSettings.setVoiceAPIEndpoint(_voiceAPIEndPoint);
            voiceBotClientSettings.setSocketEndpoint(_socketEndpoint);
            voiceBotClientSettings.setBotCode(_botCode);
        }
        return voiceBotClientSettings;
    }
}

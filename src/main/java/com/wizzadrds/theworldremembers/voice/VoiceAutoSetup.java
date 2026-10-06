package com.wizzadrds.theworldremembers.voice;

import java.util.List;

public final class VoiceAutoSetup {
    private VoiceAutoSetup() {}

    public static VoiceService create(VoiceClientConfig config, List<String> sttCommand, List<String> ttsCommand) {
        if (!sttCommand.isEmpty() && !ttsCommand.isEmpty()) {
            return new VoiceService(new LocalProcessSttAdapter(sttCommand), new LocalProcessTtsAdapter(ttsCommand));
        }
        if (config.apiKey != null && !config.apiKey.isBlank()
                && (config.provider == null || config.provider.isBlank() || config.provider.equalsIgnoreCase("openai"))) {
            OpenAiVoiceAdapter adapter = new OpenAiVoiceAdapter(
                    config.apiKey, config.sttModel, config.ttsModel, config.ttsVoice, config.ttsInstructions);
            return new VoiceService(adapter, adapter);
        }
        return null;
    }
}

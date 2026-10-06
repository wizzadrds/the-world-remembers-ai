package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.chronicle.*;
import com.wizzadrds.theworldremembers.voice.VoiceClientConfig;
import com.wizzadrds.theworldremembers.voice.VoicePacket;
import com.wizzadrds.theworldremembers.voice.VoiceSettingsScreen;
import com.wizzadrds.theworldremembers.voice.VoiceHud;
import com.wizzadrds.theworldremembers.voice.VoiceConversationController;
import com.wizzadrds.theworldremembers.voice.MicrophoneCapture;
import com.wizzadrds.theworldremembers.voice.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TheWorldRemembersClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "chronicles"));
    private static final KeyMapping CHRONICLE_KEY =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.the_world_remembers.chronicles", InputConstants.Type.KEYSYM, InputConstants.KEY_J, CATEGORY));
    private static final KeyMapping VOICE_KEY =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.the_world_remembers.voice", InputConstants.Type.KEYSYM, InputConstants.KEY_V, CATEGORY));
    private static final KeyMapping SETTINGS_KEY =
            KeyMappingHelper.registerKeyMapping(new KeyMapping("key.the_world_remembers.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY));

    private static volatile VoicePacket lastVoice;
    private static VoiceClientConfig voiceConfig;
    private static VoiceConversationController voiceConversation;
    private static MicrophoneCapture microphone;
    private static boolean voiceKeyWasDown;
    private static VoiceAudioPlayer voicePlayer;

    public static VoicePacket lastVoice() {
        return lastVoice;
    }

    public static VoiceClientConfig voiceConfig() {
        return voiceConfig;
    }

    public void onInitializeClient() {
        voiceConfig = VoiceClientConfig.load(Minecraft.getInstance().gameDirectory.toPath());
        voiceConversation = new VoiceConversationController();
        microphone = new MicrophoneCapture();
        voicePlayer = new VoiceAudioPlayer();

        VoiceHud.register(VOICE_KEY);
        ClientPlayNetworking.registerGlobalReceiver(VoicePacket.TYPE, (payload, context) -> lastVoice = payload);
        ClientPlayNetworking.registerGlobalReceiver(ChronicleResponsePacket.TYPE, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new ChronicleScreen(payload.lines()))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CHRONICLE_KEY.consumeClick()) {
                if (client.player != null) openChronicles();
            }
            while (SETTINGS_KEY.consumeClick()) {
                client.setScreen(new VoiceSettingsScreen(client.screen, voiceConfig));
            }
            if (client.player != null && client.screen == null) {
                boolean down = VOICE_KEY.isDown();
                if (down && !voiceKeyWasDown) {
                    if (microphone.start(voiceConfig.microphone)) {
                        voiceConversation.beginListening();
                    } else {
                        voiceConversation.fail();
                    }
                } else if (!down && voiceKeyWasDown) {
                    byte[] pcm = microphone.stop();
                    if (pcm.length > 0) {
                        processVoice(pcm);
                    } else {
                        voiceConversation.fail();
                    }
                }
                voiceKeyWasDown = down;
            }
        });
    }

    private static void processVoice(byte[] pcm) {
        var sttCommand = VoiceCommandParser.parse(voiceConfig.sttCommand);
        if (sttCommand.isEmpty()) { voiceConversation.fail(); return; }
        var ttsCommand = VoiceCommandParser.parse(voiceConfig.ttsCommand);
        if (ttsCommand.isEmpty()) { voiceConversation.fail(); return; }
        var service = new VoiceService(new LocalProcessSttAdapter(sttCommand), new LocalProcessTtsAdapter(ttsCommand));
        voiceConversation.finishListening(pcm, service, transcript -> {
            try {
                AiChatAdapter ai = new OpenAiResponsesAdapter(voiceConfig.apiKey, voiceConfig.model);
                String reply = ai.respond(transcript, voiceConfig.systemPrompt);
                if (reply == null || reply.isBlank()) { voiceConversation.fail(); return; }
                VoiceProfile profile = new VoiceProfile("es-ES", voiceConfig.ttsModel, VoiceTemperament.NORMAL, 1.0f, 1.0f, 0.5f);
                Path output = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("the_world_remembers_voice_response.wav");
                voiceConversation.synthesizeAndSpeak(reply, service, profile, output, voicePlayer, ignored -> {});
            } catch (Exception e) {
                voiceConversation.fail();
            }
        });
    }

    public static void openChronicles() {
        ChronicleNetworking.request();
    }
}

package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.chronicle.*;
import com.wizzadrds.theworldremembers.voice.VoiceClientConfig;
import com.wizzadrds.theworldremembers.voice.VoicePacket;
import com.wizzadrds.theworldremembers.voice.VoiceSettingsScreen;
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

    public static VoicePacket lastVoice() {
        return lastVoice;
    }

    public static VoiceClientConfig voiceConfig() {
        return voiceConfig;
    }

    public void onInitializeClient() {
        voiceConfig = VoiceClientConfig.load(Minecraft.getInstance().gameDirectory.toPath());

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
            while (VOICE_KEY.consumeClick()) {
                if (client.player != null) {
                    client.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal("🎙 Listening…"), false);
                }
            }
        });
    }

    public static void openChronicles() {
        ChronicleNetworking.request();
    }
}

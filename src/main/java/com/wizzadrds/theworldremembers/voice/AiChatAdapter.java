package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.util.function.Consumer;

public interface AiChatAdapter {
    String respond(String userText, String systemPrompt) throws IOException, InterruptedException;

    default String respondStreaming(String userText, String systemPrompt, Consumer<String> chunkConsumer) throws IOException, InterruptedException {
        String text = respond(userText, systemPrompt);
        if (chunkConsumer != null && text != null && !text.isBlank()) chunkConsumer.accept(text);
        return text;
    }
}

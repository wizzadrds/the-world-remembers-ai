package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;

public interface AiChatAdapter {
    String respond(String userText, String systemPrompt) throws IOException, InterruptedException;
}

package com.wizzadrds.theworldremembers.voice;

import java.util.ArrayList;
import java.util.List;

public final class VoiceCommandParser {
    private VoiceCommandParser() {}

    public static List<String> parse(String command) {
        List<String> parts = new ArrayList<>();
        if (command == null || command.isBlank()) return parts;
        boolean quoted = false;
        char quote = 0;
        StringBuilder current = new StringBuilder();
        for (char c : command.trim().toCharArray()) {
            if ((c == '"' || c == '\\'') ) {
                if (quoted && c == quote) quoted = false;
                else if (!quoted) { quoted = true; quote = c; }
                else current.append(c);
            } else if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) { parts.add(current.toString()); current.setLength(0); }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) parts.add(current.toString());
        return parts;
    }
}

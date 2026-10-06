package com.wizzadrds.theworldremembers.voice;

import java.util.ArrayList;
import java.util.List;

public final class VoiceCommandParser {
    private VoiceCommandParser() {}

    public static List<String> parse(String command) {
        if (command == null || command.isBlank()) return List.of();
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (quote != 0) {
                if (ch == quote) quote = 0;
                else if (ch == '\\' && i + 1 < command.length()) {
                    char next = command.charAt(++i);
                    if (next == quote || next == '\\') current.append(next);
                    else { current.append(ch); current.append(next); }
                } else current.append(ch);
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (Character.isWhitespace(ch)) {
                if (!current.isEmpty()) { args.add(current.toString()); current.setLength(0); }
            } else current.append(ch);
        }
        if (quote != 0) throw new IllegalArgumentException("Unclosed quote in voice command");
        if (!current.isEmpty()) args.add(current.toString());
        return List.copyOf(args);
    }
}
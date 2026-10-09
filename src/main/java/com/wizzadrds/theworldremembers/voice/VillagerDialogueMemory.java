package com.wizzadrds.theworldremembers.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight per-villager dialogue memory for the client voice feature.
 * Entries are keyed by the villager UUID (stable across saves) and persisted
 * locally so the feature also works on servers that do not install the mod.
 */
public final class VillagerDialogueMemory {
    private static final int MAX_TURNS = 12;
    private static final int CONTEXT_TURNS = 6;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?iu)\\b(?:me llamo|mi nombre es|puedes llamarme|llámame)\\s+"
                    + "(?!(?:un|una|el|la|aldeano|aldeana|vecino|vecina)\\b)"
                    + "([\\p{L}][\\p{L}'’-]{1,23})\\b");
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean loaded;

    private VillagerDialogueMemory() {}

    public static synchronized String name(UUID villagerId) {
        ensureLoaded();
        Entry entry = ENTRIES.get(villagerId.toString());
        return entry == null || entry.name == null ? "" : entry.name;
    }

    public static synchronized String context(UUID villagerId) {
        ensureLoaded();
        Entry entry = ENTRIES.get(villagerId.toString());
        if (entry == null || entry.turns == null || entry.turns.isEmpty()) return "";
        StringBuilder out = new StringBuilder("\n\nMEMORIA REAL DE ESTE ALDEANO (usa solo estos datos; no inventes recuerdos):");
        if (entry.name != null && !entry.name.isBlank()) {
            out.append("\n- Tu nombre establecido es ").append(entry.name).append(". No lo cambies.");
        }
        int start = Math.max(0, entry.turns.size() - CONTEXT_TURNS);
        for (int i = start; i < entry.turns.size(); i++) {
            Turn turn = entry.turns.get(i);
            if (turn == null) continue;
            out.append("\n- Jugador: ").append(compact(turn.playerText, 180));
            out.append("\n  Tú respondiste: ").append(compact(turn.villagerText, 180));
        }
        out.append("\nRecuerda el contexto anterior si es relevante y responde a la pregunta actual sin repetir todo el historial.");
        return out.toString();
    }

    public static synchronized void remember(UUID villagerId, String playerText, String villagerText) {
        if (villagerId == null || playerText == null || playerText.isBlank()
                || villagerText == null || villagerText.isBlank()) return;
        ensureLoaded();
        Entry entry = ENTRIES.computeIfAbsent(villagerId.toString(), ignored -> new Entry());
        if (entry.turns == null) entry.turns = new ArrayList<>();
        entry.turns.add(new Turn(compact(playerText, 500), compact(villagerText, 500), System.currentTimeMillis()));
        while (entry.turns.size() > MAX_TURNS) entry.turns.remove(0);
        String detected = extractVillagerName(villagerText);
        if (!detected.isBlank() && (entry.name == null || entry.name.isBlank())) entry.name = detected;
        save();
    }

    public static synchronized void setName(UUID villagerId, String name) {
        if (villagerId == null || name == null || name.isBlank()) return;
        String clean = name.trim().replaceAll("[^\\p{L}\\p{M}'’ -]", "").replaceAll("\\s+", " ");
        if (clean.isBlank() || clean.length() > 24) return;
        ensureLoaded();
        Entry entry = ENTRIES.computeIfAbsent(villagerId.toString(), ignored -> new Entry());
        entry.name = clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
        save();
    }

    public static String extractVillagerName(String dialogue) {
        if (dialogue == null || dialogue.isBlank()) return "";
        Matcher matcher = NAME_PATTERN.matcher(dialogue);
        if (!matcher.find()) return "";
        String value = matcher.group(1).trim();
        if (value.isBlank() || value.length() > 24) return "";
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static String compact(String text, int maxLength) {
        if (text == null) return "";
        String value = text.replaceAll("\\s+", " ").trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path file = file();
        try {
            if (!Files.isRegularFile(file)) return;
            Map<String, Entry> parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    new com.google.gson.reflect.TypeToken<Map<String, Entry>>() {}.getType());
            if (parsed != null) ENTRIES.putAll(parsed);
        } catch (Exception ignored) {
            // A corrupt memory file should not disable voice chat.
        }
    }

    private static void save() {
        Path file = file();
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(ENTRIES), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Keep the in-memory copy available for the current session.
        } finally {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
        }
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config")
                .resolve("the_world_remembers_dialogue_memory.json");
    }

    private static final class Entry {
        String name = "";
        List<Turn> turns = new ArrayList<>();
    }

    private static final class Turn {
        String playerText;
        String villagerText;
        long timestamp;

        Turn(String playerText, String villagerText, long timestamp) {
            this.playerText = playerText;
            this.villagerText = villagerText;
            this.timestamp = timestamp;
        }
    }
}

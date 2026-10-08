package com.wizzadrds.theworldremembers.voice;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

public interface TtsAdapter {
    Path synthesize(String text, VoiceProfile profile, Path output) throws IOException, InterruptedException;

    default InputStream synthesizeStream(String text, VoiceProfile profile) throws IOException, InterruptedException {
        return null;
    }
}
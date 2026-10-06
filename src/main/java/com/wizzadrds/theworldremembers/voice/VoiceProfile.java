package com.wizzadrds.theworldremembers.voice;
import java.util.Objects;
public record VoiceProfile(String language,String modelId,VoiceTemperament temperament,float rate,float pitch,float expressiveness){public VoiceProfile{Objects.requireNonNull(language);Objects.requireNonNull(modelId);Objects.requireNonNull(temperament);if(rate<0.5f||rate>2f||pitch<0.5f||pitch>2f||expressiveness<0||expressiveness>1)throw new IllegalArgumentException();}}

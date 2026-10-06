package com.wizzadrds.theworldremembers.voice;
public record VoiceDeliveryState(VoiceTemperament temperament,int rate,int pitch,int intensity){public VoiceDeliveryState{rate=Math.max(-100,Math.min(100,rate));pitch=Math.max(-100,Math.min(100,pitch));intensity=Math.max(0,Math.min(100,intensity));}}

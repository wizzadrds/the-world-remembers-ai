package com.wizzadrds.theworldremembers.voice; import java.util.UUID; public record VoiceJob(UUID npcId,String text,VoicePriority priority,long createdTick){}

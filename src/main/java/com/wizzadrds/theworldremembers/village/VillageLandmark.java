package com.wizzadrds.theworldremembers.village;
import net.minecraft.core.BlockPos;
public record VillageLandmark(String type, BlockPos position, long firstSeenTick) { public VillageLandmark { if(type==null||type.isBlank()) throw new IllegalArgumentException("type"); } }

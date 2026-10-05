package com.wizzadrds.theworldremembers.village;
import net.minecraft.core.BlockPos; import java.util.UUID;
public record VillageEvent(String type,long tick,UUID subject,BlockPos position){ }

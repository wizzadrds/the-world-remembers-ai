package com.wizzadrds.theworldremembers.behavior;
import net.minecraft.core.BlockPos; import java.util.UUID;
public record NpcActivityState(UUID npcId,NpcActivity activity,int priority,boolean interruptible,BlockPos target,long sinceTick){public NpcActivityState{if(priority<0)throw new IllegalArgumentException("priority");} public static NpcActivityState idle(UUID id,long tick,BlockPos pos){return new NpcActivityState(id,NpcActivity.IDLE,10,true,pos,tick);}}

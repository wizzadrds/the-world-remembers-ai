package com.wizzadrds.theworldremembers.memory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.Codec;
import java.util.*;

public final class MemoryManager extends SavedData {
    private static final int MAX_MEMORIES=4096;
    private final List<Memory> memories=new ArrayList<>();
    private static final Codec<MemoryManager> CODEC=Memory.CODEC.listOf().xmap(list->{MemoryManager x=new MemoryManager();x.memories.addAll(list);return x;},x->x.memories);
    private static final SavedDataType<MemoryManager> TYPE=new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers"), "memories",MemoryManager::new,CODEC,null);
    public static MemoryManager get(MinecraftServer server){ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);return l==null?new MemoryManager():l.getDataStorage().computeIfAbsent(TYPE);}
    public Memory rememberBreadGift(ServerPlayer player,Villager villager){return rememberEvent(villager.getUUID(),player.getUUID(),MemoryEventType.PLAYER_GAVE_BREAD,player.level().getGameTime(),MemoryImportance.IMPORTANT);}
    public Memory rememberEvent(UUID npc,UUID player,MemoryEventType type,long time,MemoryImportance importance){
        Memory m=new Memory(npc,player,type,time,importance);memories.add(m);prune();setDirty();return m;
    }
    private void prune(){while(memories.size()>MAX_MEMORIES){int idx=0;for(int i=1;i<memories.size();i++)if(memories.get(i).importance().ordinal()<memories.get(idx).importance().ordinal())idx=i;memories.remove(idx);}}
    public Optional<Memory> findMostRecentMemory(UUID npc,UUID player,MemoryEventType type){for(int i=memories.size()-1;i>=0;i--){Memory m=memories.get(i);if(m.npcId().equals(npc)&&m.playerId().equals(player)&&m.type()==type)return Optional.of(m);}return Optional.empty();}
}

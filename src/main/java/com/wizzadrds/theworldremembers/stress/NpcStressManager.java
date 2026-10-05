package com.wizzadrds.theworldremembers.stress;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcStressManager extends SavedData {
    private final Map<UUID,Integer> stress=new HashMap<>();
    private static final Codec<NpcStressManager> CODEC=Codec.unboundedMap(UUIDUtil.CODEC,Codec.INT).xmap(m->{NpcStressManager x=new NpcStressManager();x.stress.putAll(m);return x;},x->x.stress);
    private static final SavedDataType<NpcStressManager> TYPE=new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers"), "stress",NpcStressManager::new,CODEC,null);
    public static NpcStressManager get(ServerLevel world){return world.getDataStorage().computeIfAbsent(TYPE);}
    public int value(UUID id){return stress.getOrDefault(id,0);}
    public void increase(UUID id,int amount){if(amount<=0)return;int old=value(id);int next=Math.min(100,old+amount);if(next!=old){stress.put(id,next);setDirty();}}
    public void decrease(UUID id,int amount){if(amount<=0)return;int old=value(id);int next=Math.max(0,old-amount);if(next!=old){stress.put(id,next);setDirty();}}
    public void recover(UUID id,int amount){decrease(id,amount);}
}

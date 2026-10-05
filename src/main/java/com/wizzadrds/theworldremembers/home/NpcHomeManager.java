package com.wizzadrds.theworldremembers.home;

import com.mojang.serialization.Codec;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcHomeManager extends SavedData {
    private final Map<UUID,NpcHome> homes=new HashMap<>();
    private static final Codec<NpcHomeManager> CODEC=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString),NpcHome.CODEC).xmap(m->{NpcHomeManager x=new NpcHomeManager();x.homes.putAll(m);return x;},x->x.homes);
    private static final SavedDataType<NpcHomeManager> TYPE=new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "homes"),NpcHomeManager::new,CODEC,null);
    public static NpcHomeManager get(ServerLevel world){return world.getDataStorage().computeIfAbsent(TYPE);}
    public NpcHome get(UUID id){return homes.get(id);}
    public boolean hasHome(UUID id){return homes.containsKey(id);}
    public NpcHome assignIfAbsent(UUID id, net.minecraft.core.BlockPos home, net.minecraft.core.BlockPos bed, net.minecraft.core.BlockPos entrance){
        NpcHome h=homes.computeIfAbsent(id,k->new NpcHome(id,home,bed,entrance));setDirty();return h;
    }
    public void assignFamilyHome(UUID member, UUID familyMember){
        NpcHome familyHome=homes.get(familyMember);
        if(familyHome==null)return;
        NpcHome shared=new NpcHome(member,familyHome.homePos(),familyHome.bedPos(),familyHome.entrancePos());
        if(!shared.equals(homes.get(member))){homes.put(member,shared);setDirty();}
    }
}

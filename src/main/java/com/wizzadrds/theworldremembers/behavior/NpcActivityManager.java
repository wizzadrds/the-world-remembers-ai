package com.wizzadrds.theworldremembers.behavior;
import com.mojang.serialization.Codec; import com.mojang.serialization.codecs.RecordCodecBuilder; import net.minecraft.server.MinecraftServer; import net.minecraft.server.level.ServerLevel; import net.minecraft.world.level.saveddata.SavedData; import net.minecraft.world.level.saveddata.SavedDataType; import net.minecraft.resources.Identifier; import java.util.*;
public final class NpcActivityManager extends SavedData{
 private final Map<UUID,NpcActivityState> states=new HashMap<>();
 private static final Codec<NpcActivityState>S=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("activity").forGetter(x->x.activity().name()),Codec.LONG.fieldOf("sinceTick").forGetter(NpcActivityState::sinceTick)).apply(i,(a,t)->new NpcActivityState(NpcActivity.valueOf(a),t)));
 private static final Codec<NpcActivityManager>C=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString,UUID::toString),S).xmap(m->{var x=new NpcActivityManager();x.states.putAll(m);return x;},x->x.states);
 private static final SavedDataType<NpcActivityManager> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("the_world_remembers","npc_activity"),NpcActivityManager::new,C,null);
 public static NpcActivityManager get(ServerLevel w){return w.getDataStorage().computeIfAbsent(TYPE);}
 public NpcActivity activity(UUID id){return states.getOrDefault(id,new NpcActivityState(NpcActivity.IDLE,0)).activity();}
 public void set(UUID id,NpcActivity activity,long tick){var old=states.get(id);if(old==null||old.activity()!=activity){states.put(id,new NpcActivityState(activity,tick));setDirty();}}
}

package com.wizzadrds.theworldremembers.behavior;

import com.wizzadrds.theworldremembers.home.NpcHome;
import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.relationship.Relationship;
import com.wizzadrds.theworldremembers.relationship.RelationshipManager;
import com.wizzadrds.theworldremembers.stress.NpcStressManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import java.util.UUID;

public final class NpcLiveBehaviorController {
    private NpcLiveBehaviorController() {}
    public static NpcActivity activity(Villager villager) {
        if (villager.isSleeping()) return NpcActivity.SLEEPING;
        if (villager.getNavigation().isInProgress()) return NpcActivity.WALKING;
        return NpcActivity.IDLE;
    }
    public static NpcDecision decide(Villager villager, ServerPlayer player, RelationshipManager relationships, NpcStressManager stress) {
        Relationship relationship=relationships.getOrCreate(villager.getUUID(),player.getUUID());
        return new NpcBehaviorEngine().decide(activity(villager),relationship,PersonalityGenerator.generate(villager.getUUID()),new com.wizzadrds.theworldremembers.stress.NpcStress(stress.value(villager.getUUID())));
    }
    public static void apply(ServerLevel world,Villager villager,ServerPlayer player,NpcDecision decision,NpcHomeManager homes) {
        switch(decision) {
            case FOLLOW -> { villager.getNavigation().moveTo(player,1.0); }
            case LEAVE -> { var away=player.blockPosition().subtract(villager.blockPosition()); var target=villager.blockPosition().offset(Integer.signum(away.getX())*-12,0,Integer.signum(away.getZ())*-12); villager.getNavigation().moveTo(target.getX(),target.getY(),target.getZ(),1.1); }
            case RETURN_HOME -> { NpcHome home=homes.get(villager.getUUID()); if(home!=null) villager.getNavigation().moveTo(home.homePos().getX(),home.homePos().getY(),home.homePos().getZ(),1.0); }
            default -> { }
        }
    }
}

package com.github.andre2xu.endgamebosses.bosses.samurice.clone;

import com.github.andre2xu.endgamebosses.bosses.samurice.SamuriceEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SamuriceCloneEntity extends SamuriceEntity {
    public SamuriceCloneEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .build();
    }



    // BOSS HEALTHBAR (disabled)
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer pServerPlayer) {}

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer pServerPlayer) {}



    // AI
    @Override
    protected void registerGoals() {
        // target the player that hurt the clone
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new SelectTargetGoal(this));

        // handle blocking (see SamuriceEntity for the goal definition)
        this.goalSelector.addGoal(1, new BlockGoal(this));

        // handle attacks (see SamuriceEntity for the goal definitions)
        this.goalSelector.addGoal(1, new DashAttackGoal(this));
        this.goalSelector.addGoal(1, new CutsAttackGoal(this));
    }

    @Override
    public void aiStep() {}
}

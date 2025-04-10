package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Predicate;

public class MechalodonEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);

    // ANIMATIONS
    protected static final RawAnimation SWIM_FAST_ANIM = RawAnimation.begin().then("animation.mechalodon.swim_fast", Animation.LoopType.PLAY_ONCE);



    public MechalodonEntity(EntityType<? extends FlyingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10) // CHANGE LATER
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // add triggerable animations
        controllers.add(new AnimationController<>(this, "swim_fast_anim_controller", state -> PlayState.STOP).triggerableAnim("swim_fast", SWIM_FAST_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // AI
    @Override
    protected void registerGoals() {
        // find and select a target
        this.targetSelector.addGoal(1, new SelectTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        LivingEntity target = this.getTarget();

        if (target != null) {
            // move towards front of target
            Vec3 current_pos = this.position();
            Vec3 target_pos = target.position();
            Vec3 pos_in_front_of_target = target.getEyePosition().add(target.getLookAngle().scale(20));

            this.setDeltaMovement(this.getDeltaMovement().add(
                new Vec3(
                    (pos_in_front_of_target.x) - current_pos.x,
                    (target_pos.y + 4) - current_pos.y, // fly above target
                    (pos_in_front_of_target.z) - current_pos.z
                ).scale(0.1)
            ));

            // rotate yaw to face target
            double yaw_dx = target.getX() - this.getX();
            double yaw_dz = target.getZ() - this.getZ();

            float yaw_angle_towards_target = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians
            float radians_to_degrees = 180.0F / (float) Math.PI; // converts radians to degrees
            float new_yaw = -(yaw_angle_towards_target) * radians_to_degrees;

            this.setYRot(new_yaw);
            this.setYBodyRot(new_yaw);
            this.setYHeadRot(new_yaw); // required
        }
    }



    // CUSTOM GOALS
    private static class SelectTargetGoal extends NearestAttackableTargetGoal<Player> {
        public SelectTargetGoal(Mob pMob) {
            // this is a custom constructor made to reduce the amount of parameters. It doesn't override any constructor from the parent

            this(pMob, Player.class, 10, true, false, null);
        }

        public SelectTargetGoal(Mob pMob, Class<Player> pTargetType, int pRandomInterval, boolean pMustSee, boolean pMustReach, @Nullable Predicate<LivingEntity> pTargetPredicate) {
            // this is the main constructor where the target conditions are set (see NearestAttackableTargetGoal). It was overridden to increase how far the Mechalodon can spot targets

            super(pMob, pTargetType, pRandomInterval, pMustSee, pMustReach, pTargetPredicate);

            final double MAX_TARGET_DISTANCE = 50d; // blocks
            this.targetConditions = TargetingConditions.forCombat().range(MAX_TARGET_DISTANCE).selector(pTargetPredicate);
        }
    }
}

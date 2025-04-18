package com.github.andre2xu.endgamebosses.bosses.mechalodon.missile;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MechalodonMissileEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    private int auto_detonation_countdown = 20 * 30; // 5 seconds

    // DATA ACCESSORS
    private static final EntityDataAccessor<Float> BODY_PITCH = SynchedEntityData.defineId(MechalodonMissileEntity.class, EntityDataSerializers.FLOAT); // this is for adjusting the pitch of the Mechalodon missile's body in the model class



    public MechalodonMissileEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.lookControl = new MechalodonMissileLookControl(this);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1)
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // DATA
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        super.defineSynchedData(pBuilder);

        // give data accessors starting values
        pBuilder.define(BODY_PITCH, 0.0f);
    }

    public float getBodyPitch() {
        return this.entityData.get(BODY_PITCH);
    }



    // AI
    private void decrementAutoDetonationCountdown() {
        if (this.auto_detonation_countdown > 0) {
            this.auto_detonation_countdown--;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.auto_detonation_countdown > 0) {
            LivingEntity target = this.getTarget(); // target is set in the Mechalodon's missiles attack goal, just after the missile is spawned

            if (target != null && target.isAlive() && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                // look at target
                this.getLookControl().setLookAt(target);

                // move to target
                this.setDeltaMovement(target.position().subtract(this.position()).normalize().scale(0.5));
            }
        }
        else {
            System.out.println("KABOOM");

            // delete missile from game
            this.discard();
            return;
        }

        this.decrementAutoDetonationCountdown();
    }



    // CONTROLS
    private static class MechalodonMissileLookControl extends LookControl {
        public MechalodonMissileLookControl(Mob pMob) {
            super(pMob);
        }

        @Override
        public void setLookAt(@NotNull Entity pEntity) {
            super.setLookAt(pEntity); // update value of 'this.mob.getXRot'

            Vec3 target_pos = pEntity.position();

            // set yaw to face target
            double yaw_dx = target_pos.x - this.mob.getX();
            double yaw_dz = target_pos.z - this.mob.getZ();

            float yaw_angle_towards_target = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians. This formula is: θ = Tan^-1(opp/adj)
            float new_yaw = (float) Math.toDegrees(-yaw_angle_towards_target);

            this.mob.setYRot(new_yaw);
            this.mob.setYBodyRot(new_yaw);
            this.mob.setYHeadRot(new_yaw);

            // set pitch to face target
            float new_pitch = this.mob.getXRot();

            if (new_pitch > 0) {
                float pitch_adjustment = 0.2f;

                this.mob.getEntityData().set(BODY_PITCH, (float) -Math.toRadians(new_pitch) + pitch_adjustment); // GeckoLib uses radians. Rotation is done in the 'setCustomAnimations' method of the model class
            }
        }
    }
}

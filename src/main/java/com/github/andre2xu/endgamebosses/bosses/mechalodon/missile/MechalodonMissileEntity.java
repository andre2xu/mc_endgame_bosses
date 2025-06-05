package com.github.andre2xu.endgamebosses.bosses.mechalodon.missile;

import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MechalodonMissileEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    private int auto_detonation_countdown = 20 * 3; // 3 seconds

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

    private void detonate() {
        Level level = this.level();

        if (level instanceof ServerLevel) {
            level.explode(
                    this,
                    Explosion.getDefaultDamageSource(level, this),
                    new CustomExplosionDamageCalculator(), // this calculator has code to prevent accidental self harm
                    this.getX(), this.getY(), this.getZ(),
                    2, // radius
                    false, // no fire
                    Level.ExplosionInteraction.MOB
            );
        }

        // delete missile from game
        this.discard();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {
        // disable fall damage
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        // blow up when hit
        this.detonate();

        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.auto_detonation_countdown > 0) {
            // handle thruster particle
            if (this.level() instanceof ClientLevel client_level) {
                Vec3 current_pos = this.position();
                Vec3 thruster_position = current_pos.subtract(this.getLookAngle().normalize().scale(0.48)); // spawns behind missile
                Vec3 current_vector = this.getDeltaMovement().normalize().scale(-1).scale(0.2); // particles move in the opposite direction of the missile at a speed of 0.2 blocks

                client_level.addParticle(
                        ParticleTypes.FLAME,
                        thruster_position.x, current_pos.y + 0.4, thruster_position.z,
                        current_vector.x, 0, current_vector.z
                );
            }

            // handle movement
            LivingEntity target = this.getTarget(); // target is set in the Mechalodon's missiles attack goal, just after the missile is spawned

            if (target != null && target.isAlive() && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                // look at target
                this.getLookControl().setLookAt(target);

                // move to target
                this.setDeltaMovement(target.position().subtract(this.position()).normalize().scale(0.4)); // flight speed

                // play sound
                float distance_to_target = this.distanceTo(target);

                SoundEvent missile_sound = SoundEvents.NOTE_BLOCK_BIT.get();
                float missile_sound_volume = 3f;
                float missile_sound_pitch = 100f;

                if (distance_to_target <= 3 && this.auto_detonation_countdown % 2 == 0) {
                    this.playSound(missile_sound, missile_sound_volume, missile_sound_pitch);
                }
                else if (distance_to_target <= 7 && this.auto_detonation_countdown % 5 == 0) {
                    this.playSound(missile_sound, missile_sound_volume, missile_sound_pitch);
                }
                else if (this.auto_detonation_countdown % 10 == 0) {
                    this.playSound(missile_sound, missile_sound_volume, missile_sound_pitch);
                }

                // check for collision with target and detonate
                boolean has_collided_with_target = this.getBoundingBox().intersects(target.getBoundingBox());

                if (has_collided_with_target && distance_to_target <= 0) {
                    this.detonate();
                }
                else if (this.horizontalCollision || this.verticalCollision) {
                    // collided with blocks
                    this.detonate();
                }
            }
        }
        else {
            this.detonate();

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



    // MISCELLANEOUS
    private static class CustomExplosionDamageCalculator extends ExplosionDamageCalculator {
        @Override
        public boolean shouldDamageEntity(@NotNull Explosion pExplosion, @NotNull Entity pEntity) {
            if (pEntity instanceof MechalodonEntity) {
                return false;
            }

            return super.shouldDamageEntity(pExplosion, pEntity);
        }
    }
}

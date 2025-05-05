package com.github.andre2xu.endgamebosses.bosses.tragon.icicle;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class TragonIcicleEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    private final float damage = 20f;
    private boolean has_landed = false;
    private int despawn_delay = 5; // in ticks. This is needed so the icicle doesn't disappear before it hits the ground



    public TragonIcicleEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
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



    // AI
    private void generateLandingParticles(Vec3 landingPos, int radius) {
        if (this.level() instanceof ServerLevel server_level) {
            BlockPos center_pos = BlockPos.containing(landingPos).below();

            int depth = 3;

            for (int x=-radius; x <= radius; x++) {
                for (int y=-depth; y <= depth; y++) {
                    for (int z=-radius; z <= radius; z++) {
                        BlockPos block_pos = center_pos.offset(x, y, z);
                        BlockState block_state = server_level.getBlockState(block_pos);

                        boolean above_block_is_air = server_level.getBlockState(block_pos.above()).isAir();

                        if (above_block_is_air) {
                            server_level.sendParticles(
                                    new BlockParticleOption(ParticleTypes.BLOCK, block_state),
                                    block_pos.getX(), block_pos.getY(), block_pos.getZ(),
                                    10, // particle count
                                    0, 0.5, 0, // make particles move upwards
                                    0.1 // speed
                            );
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {
        // immune to fall damage
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (pSource.getEntity() == null) {
            // can be hurt by the /kill command
            return super.hurt(pSource, pAmount);
        }

        // invincible against entities
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.verticalCollision && !this.has_landed) {
            float noise_volume = 1.5f;
            this.playSound(SoundEvents.GLASS_BREAK, noise_volume, 1f);
            this.playSound(SoundEvents.GENERIC_EXPLODE.get(), noise_volume, 1f);

            Vec3 landing_pos = this.position();

            int radius = 5; // radius of blocks affected by land

            this.generateLandingParticles(landing_pos, radius);

            // hurt nearby entities
            Level level = this.level();
            AABB damage_area = this.getBoundingBox().inflate(radius, 0, radius);
            List<Entity> nearby_entities = level.getEntities(null, damage_area);

            for (Entity entity : nearby_entities) {
                if (!(entity instanceof TragonIcicleEntity) && !(entity instanceof TragonEntity)) {
                    float final_damage = this.damage;
                    double distance_from_landing_pos = Math.sqrt(entity.distanceToSqr(landing_pos));

                    if (distance_from_landing_pos > 1) {
                        // reduce damage the further away an entity is from the landing spot
                        final_damage = final_damage / (float) distance_from_landing_pos;
                    }

                    entity.hurt(this.damageSources().fallingBlock(this), final_damage);
                }
            }

            this.has_landed = true;
        }

        if (this.has_landed) {
            if (this.despawn_delay > 0) {
                this.despawn_delay--;
            }
            else {
                // disappear from game
                this.discard();
            }
        }
    }
}

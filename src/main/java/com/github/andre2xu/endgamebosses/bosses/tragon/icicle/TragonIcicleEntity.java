package com.github.andre2xu.endgamebosses.bosses.tragon.icicle;

import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
    private float damage = 0f; // dynamically adjusted (see aiStep)
    private boolean entity_was_impaled = false;
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
    private void playLandSounds() {
        float noise_volume = 1.5f;
        this.playSound(SoundEvents.GLASS_BREAK, noise_volume, 1f);

        if (!this.isInWater() && !this.isInLava() && !this.entity_was_impaled) {
            this.playSound(SoundEvents.GENERIC_EXPLODE.get(), noise_volume, 1f);
        }
    }

    private boolean canHurt(Entity entity) {
        return !(entity instanceof TragonIcicleEntity) && !(entity instanceof TragonEntity) && !(entity instanceof HitboxEntity);
    }

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
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {
        // immune to fall damage
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance pEffectInstance) {
        return false;
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

        Level level = this.level();

        // determine attack damage (relative to full un-enchanted diamond armor)
        Difficulty difficulty = level.getDifficulty();

        this.damage = switch (difficulty) {
            case Difficulty.EASY -> 45; // 3 hearts
            case Difficulty.NORMAL -> 35; // 6 hearts
            case Difficulty.HARD -> 33; // 10 hearts
            default -> 0;
        };

        // handle impaling target
        if (!this.entity_was_impaled) {
            AABB vertical_collision_box = this.getBoundingBox().inflate(0, 1, 0);
            List<Entity> entities_impaled = level.getEntities(null, vertical_collision_box);

            for (Entity entity : entities_impaled) {
                Vec3 current_pos = this.position();
                Vec3 target_head_pos = entity.getEyePosition();

                boolean target_is_below_icicle = current_pos.y > target_head_pos.y;

                if (this.canHurt(entity) && target_is_below_icicle) {
                    this.entity_was_impaled = true;
                    this.has_landed = true;

                    this.playLandSounds();

                    entity.hurt(this.damageSources().fallingBlock(this), this.damage);

                    break;
                }
            }
        }

        // handle missing target & landing instead
        if (this.verticalCollision && !this.has_landed) {
            this.playLandSounds();

            Vec3 landing_pos = this.position();

            int radius = 5; // radius of blocks affected by land

            this.generateLandingParticles(landing_pos, radius);

            // hurt nearby entities
            AABB damage_area = this.getBoundingBox().inflate(radius, 0, radius);
            List<Entity> nearby_entities = level.getEntities(null, damage_area);

            for (Entity entity : nearby_entities) {
                if (this.canHurt(entity)) {
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

        // handle despawn
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

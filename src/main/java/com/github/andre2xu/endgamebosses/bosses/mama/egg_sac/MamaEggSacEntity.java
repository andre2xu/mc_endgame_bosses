package com.github.andre2xu.endgamebosses.bosses.mama.egg_sac;

import com.github.andre2xu.endgamebosses.bosses.BossRegistry;
import com.github.andre2xu.endgamebosses.bosses.MiscEntityRegistry;
import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.spiderling.SpiderlingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MamaEggSacEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);



    public MamaEggSacEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18) // 9 hearts
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // immune to knockback
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0) // immune to explosion knockback
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
    @Override
    public boolean isPersistenceRequired() {
        // prevent despawn
        return true;
    }

    @Override
    public void checkDespawn() {
        // prevent despawn
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean ignoreExplosion(@NotNull Explosion pExplosion) {
        return true;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance pEffectInstance) {
        return false;
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        super.die(pDamageSource);

        // handle spawning
        if (this.level() instanceof ServerLevel server_level) {
            MamaEntity mama = BossRegistry.MAMA.get().create(server_level);

            if (mama != null) {
                Vec3 egg_sac_position = this.position();

                // spawn Mama
                mama.setPos(egg_sac_position);
                server_level.addFreshEntity(mama);

                // spawn her spiderlings
                int num_of_spiderlings = 30;

                for (int i=0; i < num_of_spiderlings; i++) {
                    SpiderlingEntity spiderling = MiscEntityRegistry.SPIDERLING.get().create(server_level);

                    if (spiderling != null) {
                        spiderling.setPos(egg_sac_position);
                        spiderling.setMama(mama);

                        server_level.addFreshEntity(spiderling);
                    }
                }
            }
        }

        // remove from game
        this.discard();
    }



    // SOUNDS
    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.SLIME_BLOCK_BREAK;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return SoundEvents.SLIME_HURT;
    }
}

package com.github.andre2xu.endgamebosses.bosses.tragon.icicle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TragonIcicleEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);



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
}

package com.github.andre2xu.endgamebosses.bosses.tragon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TragonEntity extends PathfinderMob implements GeoEntity {
    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);



    protected TragonEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }
}

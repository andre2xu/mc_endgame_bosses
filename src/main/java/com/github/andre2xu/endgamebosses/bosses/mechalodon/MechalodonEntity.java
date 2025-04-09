package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

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
}

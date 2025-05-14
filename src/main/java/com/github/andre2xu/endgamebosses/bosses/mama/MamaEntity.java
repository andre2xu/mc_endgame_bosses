package com.github.andre2xu.endgamebosses.bosses.mama;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MamaEntity extends PathfinderMob implements GeoEntity {
    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().then("animation.mama.walk", Animation.LoopType.PLAY_ONCE);



    public MamaEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10) // CHANGE LATER
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // add triggerable animations
        controllers.add(new AnimationController<>(this, "movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("walk", WALK_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // AI
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        // don't push players (i.e. allow them to get close)
        if (!(entity instanceof Player)) {
            super.doPush(entity);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.triggerAnim("movement_trigger_anim_controller", "walk");
    }
}

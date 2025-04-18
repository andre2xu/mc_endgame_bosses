package com.github.andre2xu.endgamebosses.bosses.mechalodon.missile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MechalodonMissileEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    private int auto_detonation_countdown = 20 * 5; // 5 seconds
    private LivingEntity target = null;



    public MechalodonMissileEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1)
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // DATA
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {

    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {

    }



    // AI
    private void decrementAutoDetonationCountdown() {
        if (this.auto_detonation_countdown > 0) {
            this.auto_detonation_countdown--;
        }
    }

    private void setTarget(LivingEntity target) {
        // call this before spawning a missile

        this.target = target;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.auto_detonation_countdown > 0 && this.target != null) {
            System.out.println("Homing in on target");
        }
        else {
            // delete missile from game
            this.discard();
            return;
        }

        this.decrementAutoDetonationCountdown();
    }
}

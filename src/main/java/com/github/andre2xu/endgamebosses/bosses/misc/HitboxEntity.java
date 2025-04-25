package com.github.andre2xu.endgamebosses.bosses.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

public class HitboxEntity extends PartEntity<PathfinderMob> {
    private final PathfinderMob parent;
    private final EntityDimensions size;
    private final String bone_name;

    public HitboxEntity(PathfinderMob parent, String boneName, float width, float height) {
        super(parent);

        this.parent = parent;

        this.bone_name = boneName; // name of model bone the hitbox is tied to

        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions(); // call 'getDimensions'
    }

    public String getBoneName() {
        return this.bone_name;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {}

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {}

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pPose) {
        return this.size;
    }

    @Override
    public boolean isPickable() {
        // required for hurt method to work
        return true;
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        // required for hurt method to work
        return this.parent.getPickResult();
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return this.parent.hurt(pSource, pAmount);
    }
}

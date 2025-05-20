package com.github.andre2xu.endgamebosses.bosses.mama.spiderling;

import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class SpiderlingEntity extends PathfinderMob implements GeoEntity {
    // GENERAL
    private Long mama_id = null;
    private MamaEntity mama = null;

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().then("animation.mama.walk", Animation.LoopType.PLAY_ONCE);



    public SpiderlingEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        // set look control to be the same as Mama's
        this.lookControl = new MamaEntity.MamaLookControl(this);
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



    // DATA
    public void setMama(MamaEntity mama) {
        // this is called in the 'die' method of the MamaEggSacEntity class, i.e. before the spiderling is spawned

        this.mama = mama;
        this.mama_id = mama.getMamaId(); // this is not a redundancy. See 'addAdditionalSaveData' & 'readAdditionalSaveData'

        this.mama.incrementChildCount();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // save Mama ID to persistent storage
        if (this.mama_id != null) {
            pCompound.putLong("mama_id", this.mama_id);
        }

        super.addAdditionalSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        // retrieve Mama ID from persistent storage
        this.mama_id = pCompound.getLong("mama_id");
    }



    // AI
    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {}

    @Override
    public void makeStuckInBlock(@NotNull BlockState pState, @NotNull Vec3 pMotionMultiplier) {
        if (pState.is(Blocks.COBWEB)) {
            // don't get stuck in cobwebs
            return;
        }

        super.makeStuckInBlock(pState, pMotionMultiplier);
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypes.CRAMMING)) {
            // don't get hurt from overcrowding
            return false;
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        super.die(pDamageSource);

        if (this.mama != null) {
            this.mama.decrementChildCount();
        }
    }

    @Override
    public void push(@NotNull Entity pEntity) {
        super.push(pEntity);

        // move legs when being pushed
        this.triggerAnim("movement_trigger_anim_controller", "walk");
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt the spiderling
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new MamaEntity.SelectTargetGoal(this)); // sharing the same target goal as Mama
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // remove Mama reference if she's dead
        if (this.mama != null && !this.mama.isAlive()) {
            this.mama = null;
        }

        // get Mama reference (this block only triggers when the world is reloaded)
        if (this.mama == null && this.mama_id != null) {
            AABB area_to_check_for_mama = this.getBoundingBox().inflate(100); // 100 blocks in the xyz axes

            List<MamaEntity> all_mama_entities_in_area = this.level().getEntitiesOfClass(MamaEntity.class, area_to_check_for_mama);

            for (MamaEntity mama_entity : all_mama_entities_in_area) {
                if (mama_entity.isAlive() && mama_entity.getMamaId() == this.mama_id) {
                    this.mama = mama_entity;
                    this.mama.incrementChildCount();
                    break;
                }
            }
        }

        // handle behaviour towards target
        LivingEntity target = this.getTarget();

        if (target != null) {
            // rotate horizontally to face target
            this.getLookControl().setLookAt(target);

            // move towards target
            float distance_to_target = this.distanceTo(target);

            if (distance_to_target > 3) {
                double speed = 0.1;

                if (distance_to_target > 10) {
                    speed = 0.5; // speed boost to catch up to target
                }

                this.setDeltaMovement(target.position().subtract(this.position()).normalize().scale(speed));

                if (this.horizontalCollision && this.onGround()) {
                    this.jumpFromGround();
                }

                this.triggerAnim("movement_trigger_anim_controller", "walk");
            }
        }
    }



    // SOUNDS
    @Override
    protected void playHurtSound(@NotNull DamageSource pSource) {
        this.playSound(SoundEvents.SPIDER_HURT, 1f, 1f);
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }
}

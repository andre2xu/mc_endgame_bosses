package com.github.andre2xu.endgamebosses.bosses.tragon;

import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;

public class TragonEntity extends PathfinderMob implements GeoEntity {
    private final PartEntity<?>[] hitboxes;

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().then("animation.tragon.walk", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().then("animation.tragon.swim", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation HIDE_ANIM = RawAnimation.begin().then("animation.tragon.hide", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation EXPOSE_ANIM = RawAnimation.begin().then("animation.tragon.expose", Animation.LoopType.HOLD_ON_LAST_FRAME);



    public TragonEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.hitboxes = new PartEntity[] {
                new HitboxEntity(this, "fire_head_neck", 2, 2),
                new HitboxEntity(this, "lightning_head_neck", 2, 2),
                new HitboxEntity(this, "ice_head_neck", 2, 2)
        };

        this.setId(ENTITY_COUNTER.getAndAdd(this.hitboxes.length + 1) + 1);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10) // CHANGE LATER
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // immune to knockback
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0) // immune to explosion knockback
                .add(Attributes.SAFE_FALL_DISTANCE, 100)
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // add triggerable animations
        controllers.add(new AnimationController<>(this, "movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("walk", WALK_ANIM)
                .triggerableAnim("swim", SWIM_ANIM)
                .triggerableAnim("hide_in_shell", HIDE_ANIM) // this is here to stop the walk & swim animations
                .triggerableAnim("come_out_of_shell", EXPOSE_ANIM) // this is here to stop the walk & swim animations
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // EXTRA HITBOXES
    @Override
    public void setId(int pId) {
        super.setId(pId);

        // FIX: giving the hitboxes their own ids is required for hurt detection to work properly (see EnderDragon class)
        for (int i = 0; i < this.hitboxes.length; i++) {
            this.hitboxes[i].setId(pId + i + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public @Nullable PartEntity<?>[] getParts() {
        return this.hitboxes;
    }

    public void updateHitboxPosition(String boneName, Vec3 bonePos) {
        // this is called in the 'setCustomAnimations' method of the model class because that's where the bone positions can be accessed

        Level level = this.level();

        for (PartEntity<?> hitbox : this.hitboxes) {
            if (Objects.equals(((HitboxEntity) hitbox).getBoneName(), boneName)) {
                if (!level.isClientSide) {
                    hitbox.setPos(bonePos);
                }
                else {
                    hitbox.moveTo(bonePos);
                }
            }
        }
    }



    // AI
    @SuppressWarnings("SimplifiableConditionalExpression")
    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return this.isInvulnerable() || this.isInvulnerableTo(pSource) ? false : super.hurt(pSource, pAmount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

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
}

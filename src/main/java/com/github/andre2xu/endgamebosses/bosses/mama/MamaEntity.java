package com.github.andre2xu.endgamebosses.bosses.mama;

import com.github.andre2xu.endgamebosses.bosses.mama.egg_sac.MamaEggSacEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.spiderling.SpiderlingEntity;
import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;
import java.util.function.Predicate;

public class MamaEntity extends PathfinderMob implements GeoEntity {
    /*
    See 'CHANGE LATER' comments

    TODO:
    - Increase MAX_HEALTH attribute
    - Increase MAX_HEALTH attribute of egg sac
    - Increase MAX_HEALTH attribute of spiderlings
    - Implement spiderling damage

    OPTIONAL:
    - Add boss music for Mama
    */



    // GENERAL
    private Long mama_id = null; // this is given to spiderlings so they know which Mama they belong to. The spiderlings increment/decrement Mama's child count so they need this id to find the correct Mama instance
    private final PartEntity<?>[] hitboxes;

    // BOSS FIGHT
    private final ServerBossEvent server_boss_event = new ServerBossEvent(
            Component.literal("Mama"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_12
    );
    private int boss_phase = 1;

    // DATA ACCESSORS
    private static final EntityDataAccessor<Integer> CHILD_COUNT = SynchedEntityData.defineId(MamaEntity.class, EntityDataSerializers.INT);

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().then("animation.mama.walk", Animation.LoopType.PLAY_ONCE);



    public MamaEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        // create hitboxes for Mama's body
        this.hitboxes = new PartEntity[] {
                new HitboxEntity(this, "head", 7, 3),
                new HitboxEntity(this, "abdomen", 7, 5)
        };

        this.setId(ENTITY_COUNTER.getAndAdd(this.hitboxes.length + 1) + 1);

        // add custom controls
        this.lookControl = new MamaLookControl(this); // change the default look control

        // generate id
        this.mama_id = System.currentTimeMillis();
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10) // CHANGE LATER
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8) // resistant to knockback but not immune
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.8) // resistant to explosion knockback but not immune
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
    public long getMamaId() {
        return this.mama_id;
    }

    public void incrementChildCount() {
        this.entityData.set(CHILD_COUNT, this.entityData.get(CHILD_COUNT) + 1);
    }

    public void decrementChildCount() {
        this.entityData.set(CHILD_COUNT, this.entityData.get(CHILD_COUNT) - 1);
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

        // update Mama ID to match the one that was saved
        this.mama_id = pCompound.getLong("mama_id");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        super.defineSynchedData(pBuilder);

        // give data accessors starting values
        pBuilder.define(CHILD_COUNT, 0);
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

    public void updateHitboxPosition(String hitboxName, Vec3 bonePos) {
        // this is called in the 'setCustomAnimations' method of the model class, and in 'ModelBonePositionsPacket::handle', because those are where the bone positions can be accessed

        Level level = this.level();

        for (PartEntity<?> hitbox : this.hitboxes) {
            if (Objects.equals(((HitboxEntity) hitbox).getHitboxName(), hitboxName)) {
                // adjust hitbox position for abdomen
                if (hitboxName.equals("abdomen")) {
                    Vec3 backward_vector = this.getLookAngle(); // forward vector
                    backward_vector = backward_vector.scale(-1); // reverse direction

                    // move hitbox back and down
                    bonePos = bonePos.add(backward_vector.normalize().multiply(1.5, 1, 1.5).subtract(0, 1, 0));
                }


                if (!level.isClientSide) {
                    hitbox.setPos(bonePos);
                }
                else {
                    hitbox.moveTo(bonePos);
                }
            }
        }
    }



    // BOSS FIGHT
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer pServerPlayer) {
        super.startSeenByPlayer(pServerPlayer);

        this.server_boss_event.addPlayer(pServerPlayer);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer pServerPlayer) {
        super.stopSeenByPlayer(pServerPlayer);

        this.server_boss_event.removePlayer(pServerPlayer);
    }



    // AI
    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        // don't push players (i.e. allow them to get close), spiderlings, or egg sac
        if (!(entity instanceof Player) && !(entity instanceof SpiderlingEntity) && !(entity instanceof MamaEggSacEntity)) {
            super.doPush(entity);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypes.CRAMMING) || pSource.is(DamageTypes.IN_WALL)) {
            // don't get hurt from overcrowding or suffocation
            return false;
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt Mama
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new SelectTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // update boss health bar
        float boss_health_remaining = this.getHealth() / this.getMaxHealth(); // in percentage
        this.server_boss_event.setProgress(boss_health_remaining);

        // update boss phase
        if (this.boss_phase == 1 && (boss_health_remaining <= 0.4 || this.entityData.get(CHILD_COUNT) <= 10)) {
            this.boss_phase = 2;
        }

        // handle movement & attack decisions
        LivingEntity target = this.getTarget();

        if (target != null) {
            // rotate horizontally to face target
            this.getLookControl().setLookAt(target);

            // move towards target but keep a distance
            if (this.distanceTo(target) > 25) {
                Vec3 current_pos = this.position();
                Vec3 target_pos = target.position();
                Vec3 vector_to_target = target_pos.subtract(current_pos);

                this.setDeltaMovement(vector_to_target.normalize().scale(1));

                if (this.horizontalCollision) {
                    this.jumpFromGround();
                }

                this.triggerAnim("movement_trigger_anim_controller", "walk");
            }
        }
    }



    // CONTROLS
    public static class MamaLookControl extends LookControl {
        public MamaLookControl(Mob pMob) {
            super(pMob);
        }

        @Override
        public void setLookAt(@NotNull Entity pEntity) {
            super.setLookAt(pEntity);

            Vec3 target_pos = pEntity.position();

            // set body yaw to face target
            double yaw_dx = target_pos.x - this.mob.getX();
            double yaw_dz = target_pos.z - this.mob.getZ();

            float yaw_angle_towards_target = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians. This formula is: θ = Tan^-1(opp/adj)
            float new_yaw = (float) Math.toDegrees(-yaw_angle_towards_target);

            this.mob.setYRot(new_yaw);
            this.mob.setYBodyRot(new_yaw);
            this.mob.setYHeadRot(new_yaw);
        }
    }



    // CUSTOM GOALS
    public static class SelectTargetGoal extends NearestAttackableTargetGoal<Player> {
        public SelectTargetGoal(Mob pMob) {
            // this is a custom constructor made to reduce the amount of parameters. It doesn't override any constructor from the parent

            this(pMob, Player.class, 10, true, false, null);
        }

        public SelectTargetGoal(Mob pMob, Class<Player> pTargetType, int pRandomInterval, boolean pMustSee, boolean pMustReach, @Nullable Predicate<LivingEntity> pTargetPredicate) {
            // this is the main constructor where the target conditions are set (see NearestAttackableTargetGoal). It was overridden to increase how far Mama can spot targets

            super(pMob, pTargetType, pRandomInterval, pMustSee, pMustReach, pTargetPredicate);

            final double MAX_TARGET_DISTANCE = 40d; // blocks
            this.targetConditions = TargetingConditions
                    .forCombat()
                    .range(MAX_TARGET_DISTANCE)
                    .selector(pTargetPredicate);
        }
    }
}

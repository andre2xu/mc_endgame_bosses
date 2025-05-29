package com.github.andre2xu.endgamebosses.bosses.samurice;

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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.function.Predicate;

public class SamuriceEntity extends PathfinderMob implements GeoEntity {
    /*
    See 'CHANGE LATER' comments

    TODO:
    - Increase MAX_HEALTH attribute

    OPTIONAL:
    - Add boss music for Samurice
    */



    // GENERAL
    private int chase_delay = 0;
    private Action.AttackType attack_type = Action.AttackType.MELEE; // this doesn't need to be synched between client and server so don't store it in an entity data accessor

    // BOSS FIGHT
    private final ServerBossEvent server_boss_event = new ServerBossEvent(
            Component.literal("Samurice"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_12
    );
    private static final EntityDataAccessor<Integer> BOSS_PHASE = SynchedEntityData.defineId(SamuriceEntity.class, EntityDataSerializers.INT); // the Samurice's phase is saved in persistent storage, which is only accessible server side, so this is used to synch the client phase value with the server side's

    // DATA ACCESSORS
    private static final EntityDataAccessor<Float> HEAD_PITCH = SynchedEntityData.defineId(SamuriceEntity.class, EntityDataSerializers.FLOAT); // this is for adjusting the pitch of the Samurice's head in the model class
    private static final EntityDataAccessor<Boolean> GUARD_IS_UP = SynchedEntityData.defineId(SamuriceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_ACTION = SynchedEntityData.defineId(SamuriceEntity.class, EntityDataSerializers.INT); // actions need to be synched between client and server for animations

    // ACTIONS
    public enum Action {;
        // these determine which attack goal is run

        public enum AttackType {
            MELEE
        }

        public enum Attack {
            NONE,

            // melee
            DASH
        }
    }

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().then("animation.samurice.run", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation DASH_ANIM = RawAnimation.begin().then("animation.samurice.dash", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().then("animation.samurice.swim", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation GUARD_UP_ANIM = RawAnimation.begin().then("animation.samurice.guard_up", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation GUARD_DOWN_ANIM = RawAnimation.begin().then("animation.samurice.guard_down", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation GUARD_UP_MOVE_ANIM = RawAnimation.begin().then("animation.samurice.guard_up_move", Animation.LoopType.LOOP);
    protected static final RawAnimation GUARD_UP_STOP_MOVING_ANIM = RawAnimation.begin().then("animation.samurice.guard_up_move", Animation.LoopType.HOLD_ON_LAST_FRAME);



    public SamuriceEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        // add custom controls
        this.lookControl = new SamuriceLookControl(this); // change the default look control
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
                .triggerableAnim("run", RUN_ANIM)
                .triggerableAnim("dash", DASH_ANIM)
                .triggerableAnim("swim", SWIM_ANIM)
                .triggerableAnim("guard_up", GUARD_UP_ANIM)
                .triggerableAnim("guard_down", GUARD_DOWN_ANIM)
                .triggerableAnim("guard_up_move", GUARD_UP_MOVE_ANIM)
                .triggerableAnim("guard_up_stop_moving", GUARD_UP_STOP_MOVING_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // DATA
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        // update boss phase
        int boss_phase = pCompound.getInt("boss_phase");

        if (boss_phase >= 1) {
            this.entityData.set(BOSS_PHASE, boss_phase);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // save boss phase
        pCompound.putInt("boss_phase", this.entityData.get(BOSS_PHASE)); // NOTE: the Samurice can heal if it's in water which can cause it to revert back to phase 1 if the world is reloaded. This is meant to prevent that

        super.addAdditionalSaveData(pCompound);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        super.defineSynchedData(pBuilder);

        // give data accessors starting values
        pBuilder.define(BOSS_PHASE, 1);
        pBuilder.define(HEAD_PITCH, 0.0f);
        pBuilder.define(GUARD_IS_UP, false);
        pBuilder.define(ATTACK_ACTION, 0); // none
    }

    public float getHeadPitch() {
        return this.entityData.get(HEAD_PITCH);
    }

    public boolean isGuardUp() {
        return this.entityData.get(GUARD_IS_UP);
    }

    public void setIsGuardUp(boolean isGuardUp) {
        this.entityData.set(GUARD_IS_UP, isGuardUp);
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
    private void setAttackAction(Action.Attack attackAction) {
        int action_id = 0; // none

        switch (attackAction) {
            case Action.Attack.DASH:
                action_id = 1;
                this.attack_type = Action.AttackType.MELEE;
                break;
            default:
        }

        this.entityData.set(ATTACK_ACTION, action_id);
    }

    private Action.Attack getAttackAction() {
        Action.Attack attack_action = Action.Attack.NONE;

        int action_id = this.entityData.get(ATTACK_ACTION);

        switch (action_id) {
            case 1:
                attack_action = Action.Attack.DASH;
                break;
            default:
        }

        return attack_action;
    }

    public Action.AttackType getAttackType() {
        return this.attack_type;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {}

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt the Samurice
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new SelectTargetGoal(this));

        /*
        HOW ATTACKING WORKS:
        - There are two types: MELEE and RANGE (see Action.AttackType enums)
        - All attack goals have Minecraft's 'TARGET' flag set which means they will conflict with the target selector goals. The priority of 1 means they will be executed instead of a target selector goal
        - Only one attack goal can run at a time so it doesn't matter that they all share the same priority number. The priority's only purpose is to stop the target selector goals when an attack goal is run
        - To determine which attack goal is run, their 'canUse' methods check which Action enums are active. These enums are set/replaced in the aiStep method
        */
        this.goalSelector.addGoal(1, new DashAttackGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // heal in water
        if (this.isInWater() && this.isAlive() && this.tickCount % 20 == 0) {
            this.setHealth(this.getHealth() + 2); // 1 heart per second
        }

        // update boss health bar
        float boss_health_remaining = this.getHealth() / this.getMaxHealth(); // in percentage
        this.server_boss_event.setProgress(boss_health_remaining);

        // update boss phase
        if (this.entityData.get(BOSS_PHASE) == 1 && boss_health_remaining <= 0.5) {
            this.entityData.set(BOSS_PHASE, 2);
        }

        // handle movement & attack decisions
        LivingEntity target = this.getTarget();

        if (target != null) {
            this.getLookControl().setLookAt(target);

            Vec3 target_pos = target.position();
            Vec3 current_pos = this.position();
            Vec3 vector_to_target = target_pos.subtract(current_pos);

            boolean same_xz_position_as_target = Math.abs(current_pos.x - target_pos.x) <= 0.5 && Math.abs(current_pos.z - target_pos.z) <= 0.5;

            if (this.distanceTo(target) > 6) {
                // put guard down before running
                if (this.isGuardUp()) {
                    this.triggerAnim("movement_trigger_anim_controller", "guard_down");
                    this.setIsGuardUp(false);
                }

                // run towards target
                if (this.chase_delay > 0) {
                    this.chase_delay--;
                }
                else {
                    if (Math.abs(this.getY() - target.getY()) <= 1) {
                        this.getNavigation().stop();

                        if (vector_to_target.y > 0) {
                            // prevent hovering
                            vector_to_target = vector_to_target.multiply(1, 0, 1);
                        }

                        this.setDeltaMovement(vector_to_target.normalize().scale(0.6));

                        if (this.horizontalCollision) {
                            this.jumpFromGround();
                        }
                    }
                    else if (this.isInFluidType()) {
                        this.getNavigation().stop();
                        this.setDeltaMovement(vector_to_target.normalize().scale(0.3));

                        if (this.horizontalCollision) {
                            this.getJumpControl().jump();
                        }
                    }
                    else {
                        this.getNavigation().moveTo(target, 0.8);
                    }

                    if (!same_xz_position_as_target) {
                        if (this.isInFluidType() && this.isUnderWater()) {
                            this.triggerAnim("movement_trigger_anim_controller", "swim");
                        }
                        else {
                            this.triggerAnim("movement_trigger_anim_controller", "run");
                        }
                    }
                }
            }
            else {
                // OBJECTIVE: When close to the target, put guard up and slowly move towards them

                if (!this.isGuardUp()) {
                    this.triggerAnim("movement_trigger_anim_controller", "guard_up");
                    this.setIsGuardUp(true);

                    this.chase_delay = 20; // let target move far away for 1 second and then run towards them
                }
                else {
                    if (this.distanceTo(target) > 3) {
                        if (Math.abs(this.getY() - target.getY()) <= 1) {
                            this.getNavigation().stop();

                            if (vector_to_target.y > 0) {
                                // prevent hovering
                                vector_to_target = vector_to_target.multiply(1, 0, 1);
                            }

                            this.setDeltaMovement(vector_to_target.normalize().scale(0.2));

                            if (this.horizontalCollision) {
                                this.jumpFromGround();
                            }
                        }
                        else if (this.isInFluidType()) {
                            this.getNavigation().stop();
                            this.setDeltaMovement(vector_to_target.normalize().scale(0.1));

                            if (this.horizontalCollision) {
                                this.getJumpControl().jump();
                            }
                        }
                        else {
                            this.getNavigation().moveTo(target, 0.4);
                        }

                        if (!same_xz_position_as_target) {
                            this.triggerAnim("movement_trigger_anim_controller", "guard_up_move");
                        }
                    }
                    else {
                        // hold guard position when very close to the target
                        this.triggerAnim("movement_trigger_anim_controller", "guard_up_stop_moving");
                    }
                }
            }
        }
        else {
            // put guard down since there's no target
            if (this.isGuardUp()) {
                this.triggerAnim("movement_trigger_anim_controller", "guard_down");
                this.setIsGuardUp(false);
            }
        }
    }



    // CONTROLS
    private static class SamuriceLookControl extends LookControl {
        public SamuriceLookControl(Mob pMob) {
            super(pMob);
        }

        @Override
        public void setLookAt(@NotNull Entity pEntity) {
            super.setLookAt(pEntity); // update value of 'this.mob.getXRot'

            Vec3 target_pos = pEntity.position();

            // set body yaw to face target
            double yaw_dx = target_pos.x - this.mob.getX();
            double yaw_dz = target_pos.z - this.mob.getZ();

            float yaw_angle_towards_target = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians. This formula is: θ = Tan^-1(opp/adj)
            float new_yaw = (float) Math.toDegrees(-yaw_angle_towards_target);

            this.mob.setYRot(new_yaw);
            this.mob.setYBodyRot(new_yaw);
            this.mob.setYHeadRot(new_yaw);

            // set pitch to face target
            float new_pitch = this.mob.getXRot();

            if (new_pitch != 0) {
                this.mob.getEntityData().set(HEAD_PITCH, (float) -Math.toRadians(new_pitch)); // GeckoLib uses radians. Rotation is done in the 'setCustomAnimations' method of the model class
            }
        }
    }



    // CUSTOM GOALS
    public static class SelectTargetGoal extends NearestAttackableTargetGoal<Player> {
        public SelectTargetGoal(Mob pMob) {
            // this is a custom constructor made to reduce the amount of parameters. It doesn't override any constructor from the parent

            this(pMob, Player.class, 10, true, false, null);
        }

        public SelectTargetGoal(Mob pMob, Class<Player> pTargetType, int pRandomInterval, boolean pMustSee, boolean pMustReach, @Nullable Predicate<LivingEntity> pTargetPredicate) {
            // this is the main constructor where the target conditions are set (see NearestAttackableTargetGoal). It was overridden to increase how far the Samurice can spot targets

            super(pMob, pTargetType, pRandomInterval, pMustSee, pMustReach, pTargetPredicate);

            final double MAX_TARGET_DISTANCE = 60d; // blocks
            this.targetConditions = TargetingConditions
                    .forCombat()
                    .ignoreLineOfSight() // allow the Samurice to see a target even if there's obstructions in the way, e.g. trees, small hills, etc.
                    .range(MAX_TARGET_DISTANCE)
                    .selector(pTargetPredicate);
        }
    }

    private static class DashAttackGoal extends Goal {
        private final SamuriceEntity samurice;
        private LivingEntity target = null;
        private boolean attack_is_finished = false;

        public DashAttackGoal(SamuriceEntity samurice) {
            this.samurice = samurice;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void resetAttack() {
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.samurice.getTarget' which can sometimes return null
            this.target = this.samurice.getTarget();

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            super.stop();
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.samurice.getAttackType() == Action.AttackType.MELEE && this.samurice.getAttackAction() == Action.Attack.DASH;
        }
    }
}

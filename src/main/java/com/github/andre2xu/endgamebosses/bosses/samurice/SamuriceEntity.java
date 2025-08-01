package com.github.andre2xu.endgamebosses.bosses.samurice;

import com.github.andre2xu.endgamebosses.bosses.MiscEntityRegistry;
import com.github.andre2xu.endgamebosses.bosses.samurice.clone.SamuriceCloneEntity;
import com.github.andre2xu.endgamebosses.data.BossStateData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

public class SamuriceEntity extends PathfinderMob implements GeoEntity {
    // GENERAL
    protected int chase_delay = 0;
    private Action.AttackType attack_type = Action.AttackType.MELEE;

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
    private static final EntityDataAccessor<Boolean> BLOCKING_ATTACKS = SynchedEntityData.defineId(SamuriceEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_ACTION = SynchedEntityData.defineId(SamuriceEntity.class, EntityDataSerializers.INT);

    // ACTIONS
    public enum Action {;
        // these determine which attack goal is run

        public enum AttackType {
            MELEE,
            SUMMON
        }

        public enum Attack {
            NONE,

            // melee
            DASH,
            CUTS,

            // summon
            CLONES // only in phase 2
        }
    }

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().then("animation.samurice.run", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation DASH_ANIM = RawAnimation.begin().then("animation.samurice.dash", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation DASH_RESET_ANIM = RawAnimation.begin().then("animation.samurice.dash_reset", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().then("animation.samurice.swim", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation GUARD_UP_ANIM = RawAnimation.begin().then("animation.samurice.guard_up", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation GUARD_DOWN_ANIM = RawAnimation.begin().then("animation.samurice.guard_down", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation GUARD_UP_MOVE_ANIM = RawAnimation.begin().then("animation.samurice.guard_up_move", Animation.LoopType.LOOP);
    protected static final RawAnimation GUARD_UP_STOP_MOVING_ANIM = RawAnimation.begin().then("animation.samurice.guard_up_move", Animation.LoopType.HOLD_ON_LAST_FRAME);

    protected static final RawAnimation HORIZONTAL_CUT_ANIM = RawAnimation.begin().then("animation.samurice.horizontal_cut", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation DIAGONAL_CUT_ANIM = RawAnimation.begin().then("animation.samurice.diagonal_cut", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation DOWNWARD_CUT_ANIM = RawAnimation.begin().then("animation.samurice.downward_cut", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation SUMMON_ANIM = RawAnimation.begin().then("animation.samurice.summon", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation SUMMON_RESET_ANIM = RawAnimation.begin().then("animation.samurice.summon_reset", Animation.LoopType.PLAY_ONCE);

    protected static final RawAnimation BLOCK_ANIM = RawAnimation.begin().then("animation.samurice.block", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation UNBLOCK_ANIM = RawAnimation.begin().then("animation.samurice.unblock", Animation.LoopType.PLAY_ONCE);



    public SamuriceEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        // add custom controls
        this.lookControl = new SamuriceLookControl(this); // change the default look control
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200) // 100 hearts
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // add triggerable animations
        controllers.add(new AnimationController<>(this, "movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("run", RUN_ANIM)
                .triggerableAnim("dash", DASH_ANIM)
                .triggerableAnim("dash_reset", DASH_RESET_ANIM)
                .triggerableAnim("swim", SWIM_ANIM)
                .triggerableAnim("guard_up", GUARD_UP_ANIM)
                .triggerableAnim("guard_down", GUARD_DOWN_ANIM)
                .triggerableAnim("guard_up_move", GUARD_UP_MOVE_ANIM)
                .triggerableAnim("guard_up_stop_moving", GUARD_UP_STOP_MOVING_ANIM)
        );

        controllers.add(new AnimationController<>(this, "attack_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("horizontal_cut", HORIZONTAL_CUT_ANIM)
                .triggerableAnim("diagonal_cut", DIAGONAL_CUT_ANIM)
                .triggerableAnim("downward_cut", DOWNWARD_CUT_ANIM)
                .triggerableAnim("block", BLOCK_ANIM) // this is here to stop the attack animations
                .triggerableAnim("unblock", UNBLOCK_ANIM) // this is here to stop the attack animations
                .triggerableAnim("summon", SUMMON_ANIM)
                .triggerableAnim("summon_reset", SUMMON_RESET_ANIM)
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
        pBuilder.define(BLOCKING_ATTACKS, false);
        pBuilder.define(ATTACK_ACTION, 0); // none
    }

    public float getHeadPitch() {
        return this.entityData.get(HEAD_PITCH);
    }

    protected boolean isGuardUp() {
        return this.entityData.get(GUARD_IS_UP);
    }

    protected void setIsGuardUp(boolean isGuardUp) {
        this.entityData.set(GUARD_IS_UP, isGuardUp);
    }

    protected boolean isBlockingAttacks() {
        return this.entityData.get(BLOCKING_ATTACKS);
    }

    protected void setIsBlockingAttacks(boolean isBlocking) {
        this.entityData.set(BLOCKING_ATTACKS, isBlocking);
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
    protected boolean isWithinGuardDistance(LivingEntity entity) {
        return this.distanceTo(entity) <= 6;
    }

    protected void followTarget(LivingEntity target, double speed) {
        Vec3 vector_to_target = target.position().subtract(this.position());

        if (Math.abs(this.getY() - target.getY()) <= 1) {
            this.getNavigation().stop();

            if (vector_to_target.y > 0) {
                // prevent hovering
                vector_to_target = vector_to_target.multiply(1, 0, 1);
            }

            this.setDeltaMovement(vector_to_target.normalize().scale(speed));

            if (this.horizontalCollision) {
                this.jumpFromGround();
            }
        }
        else if (this.isInFluidType()) {
            this.getNavigation().stop();
            this.setDeltaMovement(vector_to_target.normalize().scale(speed / 2));

            if (this.horizontalCollision) {
                this.getJumpControl().jump();
            }
        }
        else {
            this.getNavigation().moveTo(target, speed + 0.2);
        }
    }

    protected void applyFrostTo(LivingEntity entity, int duration) {
        int frost_effect_duration = 20 * duration; // seconds

        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, frost_effect_duration));
        entity.setTicksFrozen(frost_effect_duration);
    }

    protected void runBaseAiStep() {
        // this is only for the clone
        super.aiStep();
    }

    protected void setAttackAction(Action.Attack attackAction) {
        int action_id = 0; // none

        switch (attackAction) {
            case Action.Attack.DASH:
                action_id = 1;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.CUTS:
                action_id = 2;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.CLONES:
                action_id = 3;
                this.attack_type = Action.AttackType.SUMMON;
                break;
            default:
        }

        this.entityData.set(ATTACK_ACTION, action_id);
    }

    protected Action.Attack getAttackAction() {
        Action.Attack attack_action = Action.Attack.NONE;

        int action_id = this.entityData.get(ATTACK_ACTION);

        switch (action_id) {
            case 1:
                attack_action = Action.Attack.DASH;
                break;
            case 2:
                attack_action = Action.Attack.CUTS;
                break;
            case 3:
                attack_action = Action.Attack.CLONES;
                break;
            default:
        }

        return attack_action;
    }

    protected Action.AttackType getAttackType() {
        return this.attack_type;
    }

    @Override
    protected int getBaseExperienceReward() {
        int xp = 0;

        if (this.level() instanceof ServerLevel server_level) {
            Difficulty difficulty = server_level.getDifficulty();

            xp = switch (difficulty) {
                case Difficulty.EASY -> 500;
                case Difficulty.NORMAL -> 1000;
                case Difficulty.HARD -> 2000;
                default -> xp;
            };
        }

        return xp;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance pEffectInstance) {
        // non-organic so immune to all potions
        return false;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {}

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        boolean should_block = new Random().nextInt(1, 6) == 1;

        Action.Attack current_attack = this.getAttackAction();

        if (should_block && (current_attack == Action.Attack.NONE || current_attack == Action.Attack.CUTS)) {
            this.setIsBlockingAttacks(true);
        }

        // play damage sound
        if (this.isBlockingAttacks()) {
            this.playHurtSound(pSource);
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        // get rid of clones upon death
        if (!(this instanceof SamuriceCloneEntity) && this.level() instanceof ServerLevel server_level) {
            AABB surrounding_area = this.getBoundingBox().inflate(100);

            List<SamuriceCloneEntity> clones = server_level.getEntitiesOfClass(SamuriceCloneEntity.class, surrounding_area);

            for (SamuriceCloneEntity clone : clones) {
                clone.kill();
            }
        }

        // play death sound
        this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1f, 0.4f);

        super.die(pDamageSource);
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        MinecraftServer server = this.getServer();

        if (server != null && (pReason == RemovalReason.KILLED || pReason == RemovalReason.DISCARDED)) {
            BossStateData boss_state_data = BossStateData.createOrGet(server);
            String active_boss = boss_state_data.getActiveBoss();

            String boss_name = "samurice";

            if (Objects.equals(active_boss, boss_name)) {
                if (pReason == RemovalReason.KILLED) {
                    boss_state_data.setBossState(boss_name, BossStateData.State.DEAD);
                }

                boss_state_data.setActiveBoss(null);
            }
        }

        super.remove(pReason);
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt the Samurice
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new SelectTargetGoal(this));

        // handle blocking
        this.goalSelector.addGoal(1, new BlockGoal(this));

        /*
        HOW ATTACKING WORKS:
        - There are two types: MELEE and SUMMON (see Action.AttackType enums)
        - All attack goals have Minecraft's 'TARGET' flag set which means they will conflict with the target selector goals. The priority of 1 means they will be executed instead of a target selector goal
        - Only one attack goal can run at a time so it doesn't matter that they all share the same priority number. The priority's only purpose is to stop the target selector goals when an attack goal is run
        - To determine which attack goal is run, their 'canUse' methods check which Action enums are active. These enums are set/replaced in the aiStep method
        */
        this.goalSelector.addGoal(1, new DashAttackGoal(this));
        this.goalSelector.addGoal(1, new CutsAttackGoal(this));
        this.goalSelector.addGoal(1, new SummonClones(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // heal in water
        if (this.isInWater() && this.isAlive() && this.tickCount % 20 == 0) {
            this.setHealth(this.getHealth() + (2 * 5)); // 5 hearts per second
        }

        // update boss health bar
        float boss_health_remaining = this.getHealth() / this.getMaxHealth(); // in percentage
        this.server_boss_event.setProgress(boss_health_remaining);

        // update boss phase
        if (this.entityData.get(BOSS_PHASE) == 1 && boss_health_remaining <= 0.7) {
            this.entityData.set(BOSS_PHASE, 2);
        }

        // handle movement & attack decisions
        LivingEntity target = this.getTarget();

        if (target != null) {
            if (!target.isFallFlying()) {
                boolean is_attacking = this.getAttackAction() != Action.Attack.NONE;

                if (!is_attacking && !this.isBlockingAttacks()) {
                    this.getLookControl().setLookAt(target);

                    Vec3 target_pos = target.position();
                    Vec3 current_pos = this.position();

                    boolean same_xz_position_as_target = Math.abs(current_pos.x - target_pos.x) <= 0.5 && Math.abs(current_pos.z - target_pos.z) <= 0.5;

                    if (!this.isWithinGuardDistance(target)) {
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
                            this.followTarget(target, 0.6);

                            if (!same_xz_position_as_target) {
                                if (this.isInFluidType() && this.isUnderWater()) {
                                    this.triggerAnim("movement_trigger_anim_controller", "swim");
                                }
                                else {
                                    this.triggerAnim("movement_trigger_anim_controller", "run");
                                }
                            }

                            // decide whether to dash towards target or not
                            boolean should_dash_towards_target = new Random().nextInt(1, 11) == 1; // 1 in 10 chances

                            if (should_dash_towards_target) {
                                this.setAttackAction(Action.Attack.DASH);
                            }
                        }
                    }
                    else {
                        // OBJECTIVE: When close to the target, put guard up and slowly move towards them

                        if (!this.isGuardUp()) {
                            this.triggerAnim("movement_trigger_anim_controller", "guard_up");
                            this.setIsGuardUp(true);

                            this.chase_delay = 20; // let target move far away for 1 second and then run towards them (this is for later)
                        }
                        else {
                            if (this.distanceTo(target) > 3) {
                                this.followTarget(target, 0.2);

                                if (!same_xz_position_as_target) {
                                    this.triggerAnim("movement_trigger_anim_controller", "guard_up_move");
                                }
                            }
                            else {
                                // hold guard position when very close to the target
                                this.triggerAnim("movement_trigger_anim_controller", "guard_up_stop_moving");

                                // decide whether to attack or not
                                boolean should_attack = new Random().nextInt(1, 11) == 1; // 1 in 10 chances

                                if (should_attack) {
                                    int boss_phase = this.entityData.get(BOSS_PHASE);

                                    if (boss_phase == 1) {
                                        this.setAttackAction(Action.Attack.CUTS);
                                    }
                                    else if (boss_phase == 2) {
                                        int random_number = new Random().nextInt(1, 4);

                                        switch (random_number) {
                                            case 1:
                                                this.setAttackAction(Action.Attack.CUTS);
                                                break;
                                            case 2:
                                            case 3:
                                                this.setAttackAction(Action.Attack.CLONES);
                                                break;
                                            default:
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                // watch target
                this.getLookControl().setLookAt(target);
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



    // SOUNDS
    @Override
    protected void playHurtSound(@NotNull DamageSource pSource) {
        this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1f, 1f);
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }



    // CONTROLS
    protected static class SamuriceLookControl extends LookControl {
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
    protected static class SelectTargetGoal extends NearestAttackableTargetGoal<Player> {
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

    protected static class DashAttackGoal extends Goal {
        private final SamuriceEntity samurice;
        private LivingEntity target = null;
        private boolean has_dashed = false;
        private int pose_duration = 0; // how long in ticks the Samurice will stay in the dash pose at the end of the attack
        private boolean attack_is_finished = false;

        public DashAttackGoal(SamuriceEntity samurice) {
            this.samurice = samurice;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void hurtTarget() {
            // determine attack damage (relative to full un-enchanted diamond armor)
            Difficulty difficulty = this.samurice.level().getDifficulty();

            float attack_damage = switch (difficulty) {
                case Difficulty.EASY -> 26; // 2 hearts
                case Difficulty.NORMAL -> 25; // 5.5 hearts
                case Difficulty.HARD -> 25; // 10 hearts
                default -> 0;
            };

            // damage target & apply a frost effect
            this.target.hurt(this.samurice.damageSources().mobAttack(this.samurice), attack_damage);
            this.samurice.applyFrostTo(this.target, 5);
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && this.samurice.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator())) && !this.samurice.isBlockingAttacks();
        }

        private void resetAttack() {
            this.has_dashed = false;
            this.pose_duration = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // cancel any navigation
            this.samurice.getNavigation().stop();

            // save a reference of the target to avoid having to call 'this.samurice.getTarget' which can sometimes return null
            this.target = this.samurice.getTarget();

            // set pose duration
            this.pose_duration = 20; // 1 second

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.samurice.setAttackAction(Action.Attack.NONE); // allow the Samurice to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                if (!this.has_dashed) {
                    Vec3 target_pos = this.target.position();
                    Vec3 current_pos = this.samurice.position();
                    Vec3 vector_to_target = target_pos.subtract(current_pos).normalize();

                    int num_of_blocks_ahead_of_target = 1;

                    Vec3 position_ahead_of_target = target_pos.add(vector_to_target.multiply(num_of_blocks_ahead_of_target, 1, num_of_blocks_ahead_of_target));

                    // check if the end position will make the Samurice end up below the surface and if so correct it
                    if (this.samurice.level() instanceof ServerLevel server_level) {
                        BlockPos landing_spot = BlockPos.containing(position_ahead_of_target);

                        boolean landing_spot_is_below_surface = server_level.getBlockState(landing_spot).isSolidRender(server_level, landing_spot);

                        if (landing_spot_is_below_surface) {
                            position_ahead_of_target = landing_spot.above().getCenter();
                        }
                    }

                    // perform dash
                    this.samurice.getLookControl().setLookAt(this.target);
                    this.samurice.moveTo(position_ahead_of_target);
                    this.samurice.triggerAnim("movement_trigger_anim_controller", "dash");
                    this.samurice.playSound(SoundEvents.HORSE_BREATHE, 1f, 0.8f);

                    this.has_dashed = true;

                    this.hurtTarget();
                }

                if (this.pose_duration > 0) {
                    this.pose_duration--;

                    if (this.pose_duration == 10) {
                        this.samurice.triggerAnim("movement_trigger_anim_controller", "dash_reset");
                    }
                }
                else {
                    // stop attack
                    this.attack_is_finished = true;
                }
            }
            else {
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode. Do the same if the Samurice is blocking
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.samurice.getAttackType() == Action.AttackType.MELEE && this.samurice.getAttackAction() == Action.Attack.DASH;
        }
    }

    protected static class CutsAttackGoal extends Goal {
        private final SamuriceEntity samurice;
        private LivingEntity target = null;
        private int num_of_cuts = 0;
        private int cut_duration = 0; // in ticks. This determines how long until the Samurice can follow the target again
        private boolean attack_is_finished = false;

        public CutsAttackGoal(SamuriceEntity samurice) {
            this.samurice = samurice;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void hurtTarget() {
            // determine attack damage (relative to full un-enchanted diamond armor)
            Difficulty difficulty = this.samurice.level().getDifficulty();

            float attack_damage = switch (difficulty) {
                case Difficulty.EASY -> 15; // 1 heart
                case Difficulty.NORMAL -> 20; // 4 hearts
                case Difficulty.HARD -> 20; // 6.5 hearts
                default -> 0;
            };

            // damage target & apply a frost effect
            if (this.targetIsWithinWeaponReach()) {
                this.target.hurt(this.samurice.damageSources().mobAttack(this.samurice), attack_damage);
                this.samurice.applyFrostTo(this.target, 5);
            }
        }

        private boolean targetIsWithinWeaponReach() {
            return this.target != null && this.samurice.distanceTo(target) <= 2;
        }

        private void cutTarget() {
            int random_number = new Random().nextInt(1, 4);

            switch (random_number) {
                case 1:
                    this.samurice.triggerAnim("attack_trigger_anim_controller", "horizontal_cut");
                    this.cut_duration = 20;
                    break;
                case 2:
                    this.samurice.triggerAnim("attack_trigger_anim_controller", "diagonal_cut");
                    this.cut_duration = 15;
                    break;
                case 3:
                    this.samurice.triggerAnim("attack_trigger_anim_controller", "downward_cut");
                    this.cut_duration = 15;
                    break;
                default:
            }

            // play sword swing sound
            this.samurice.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1f);

            // apply damage & status effect(s) to target
            this.hurtTarget();

            // reduce number of cuts left
            if (this.num_of_cuts > 0) {
                this.num_of_cuts--;
            }
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && this.samurice.isAlive() && this.samurice.isWithinGuardDistance(this.target) && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator())) && !this.samurice.isBlockingAttacks();
        }

        private void resetAttack() {
            this.num_of_cuts = 0;
            this.cut_duration = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // cancel any navigation
            this.samurice.getNavigation().stop();

            // save a reference of the target to avoid having to call 'this.samurice.getTarget' which can sometimes return null
            this.target = this.samurice.getTarget();

            // decide the number of cuts to make
            if (this.num_of_cuts == 0) {
                this.num_of_cuts = new Random().nextInt(2, 4); // 2 to 3 cuts
            }

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.samurice.setAttackAction(Action.Attack.NONE); // allow the Samurice to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                // OBJECTIVE: Get close to the target and perform a cut. Repeat until the no. cuts to make is zero or the target moves far away

                if (this.cut_duration > 0) {
                    this.cut_duration--;
                }
                else {
                    if (this.num_of_cuts > 0) {
                        // put guard up if it's down
                        if (!this.samurice.isGuardUp()) {
                            this.samurice.triggerAnim("movement_trigger_anim_controller", "guard_up");
                            this.samurice.setIsGuardUp(true);
                        }

                        // face target
                        this.samurice.getLookControl().setLookAt(this.target);

                        // get closer to target
                        if (!this.targetIsWithinWeaponReach()) {
                            this.samurice.followTarget(this.target, 0.3);

                            // play move animation
                            Vec3 current_pos = this.samurice.position();
                            Vec3 target_pos = this.target.position();
                            boolean same_xz_position_as_target = Math.abs(current_pos.x - target_pos.x) <= 0.5 && Math.abs(current_pos.z - target_pos.z) <= 0.5;

                            if (!same_xz_position_as_target) {
                                this.samurice.triggerAnim("movement_trigger_anim_controller", "guard_up_move");
                            }
                        }
                        else {
                            this.samurice.triggerAnim("movement_trigger_anim_controller", "guard_up_stop_moving");

                            this.cutTarget();
                        }
                    }
                    else {
                        // stop attack
                        this.attack_is_finished = true;
                    }
                }
            }
            else {
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode. Do the same if the Samurice is blocking
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.samurice.getAttackType() == Action.AttackType.MELEE && this.samurice.getAttackAction() == Action.Attack.CUTS;
        }
    }

    protected static class BlockGoal extends Goal {
        private final SamuriceEntity samurice;
        private int block_duration = 0;
        private int block_delay = 0;
        private boolean is_blocking = false;
        private boolean block_is_finished = false;

        public BlockGoal(SamuriceEntity samurice) {
            this.samurice = samurice;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void resetData() {
            this.block_duration = 0;
            this.block_delay = 0;
            this.is_blocking = false;
            this.block_is_finished = false;
        }

        @Override
        public void start() {
            if (!this.samurice.isGuardUp()) {
                // put guard up if it's down

                this.samurice.triggerAnim("movement_trigger_anim_controller", "guard_up");
                this.samurice.setIsGuardUp(true);

                this.block_delay = 10; // 0.5 seconds. This is the amount of time (in ticks) it takes the guard up animation to finish
            }
            else {
                // stop moving but keep guard up
                this.samurice.triggerAnim("movement_trigger_anim_controller", "guard_up_stop_moving");
            }

            this.block_duration = 20 * new Random().nextInt(2, 5); // 2 to 4 seconds

            super.start();
        }

        @Override
        public void stop() {
            this.resetData();

            this.samurice.setIsBlockingAttacks(false); // allow the Samurice to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (!this.samurice.isAlive()) {
                this.block_is_finished = true;
                return;
            }

            if (this.block_delay > 0) {
                this.block_delay--;
            }
            else {
                // switch to blocking pose
                if (!this.is_blocking) {
                    this.samurice.triggerAnim("attack_trigger_anim_controller", "block");
                    this.is_blocking = true;

                    this.samurice.setInvulnerable(true);
                }

                // face attacker (this can change so it's not cached)
                LivingEntity attacker = this.samurice.getTarget();

                if (attacker != null) {
                    this.samurice.getLookControl().setLookAt(attacker);
                }

                // decrease duration
                if (this.block_duration > 0) {
                    this.block_duration--;

                    // go out of blocking pose when there's 0.5 seconds left (same time it takes to unblock)
                    if (this.block_duration == 10) {
                        this.samurice.triggerAnim("attack_trigger_anim_controller", "unblock");
                        this.samurice.setInvulnerable(false);
                    }
                }
                else {
                    // stop blocking
                    this.block_is_finished = true;
                }
            }
        }

        @Override
        public boolean canUse() {
            return !this.block_is_finished && this.samurice.isBlockingAttacks();
        }
    }

    private static class SummonClones extends Goal {
        private final SamuriceEntity samurice;
        private int num_of_clones = 0;
        private int summon_delay = 0;
        private int attack_cooldown = 0;
        private boolean attack_is_finished = false;

        public SummonClones(SamuriceEntity samurice) {
            this.samurice = samurice;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void decrementAttackCooldown() {
            if (this.attack_cooldown > 0) {
                this.attack_cooldown--;

                this.samurice.setAttackAction(Action.Attack.NONE); // ensure the Samurice is allowed to a follow target & make attack decisions while there's a cooldown
            }
        }

        private void generateParticles() {
            // surround body with ice particles

            if (this.samurice.level() instanceof ServerLevel server_level) {
                Vec3 current_pos = this.samurice.position();

                for (int i=0; i < 3; i++) {
                    server_level.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
                            current_pos.x, current_pos.y + i, current_pos.z,
                            3, // particle count
                            0, 0, 0, // particle offset
                            0 // speed
                    );
                }
            }
        }

        private void resetAttack() {
            this.summon_delay = 0;
            this.attack_cooldown = 20 * 5; // 5 seconds
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // set the no. clones to summon
            this.num_of_clones = new Random().nextInt(2, 4); // 2 to 3 clones

            // get into summon pose
            this.samurice.triggerAnim("attack_trigger_anim_controller", "summon");

            // set summon delay
            this.summon_delay = 20 * 2; // 2 seconds

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.samurice.setAttackAction(Action.Attack.NONE); // allow the Samurice to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (!this.samurice.isAlive()) {
                this.attack_is_finished = true;
                return;
            }

            this.generateParticles();

            if (this.summon_delay > 0) {
                this.summon_delay--;
            }
            else {
                if (this.num_of_clones > 0) {
                    if (this.samurice.level() instanceof ServerLevel server_level) {
                        // spawn clone
                        SamuriceCloneEntity clone = MiscEntityRegistry.SAMURICE_CLONE.get().create(server_level);

                        if (clone != null) {
                            clone.setPos(this.samurice.position());

                            server_level.addFreshEntity(clone);
                        }
                    }

                    this.num_of_clones--;
                }
                else {
                    // get out of summon pose
                    this.samurice.triggerAnim("attack_trigger_anim_controller", "summon_reset");

                    this.attack_is_finished = true;
                }
            }
        }

        @Override
        public boolean canUse() {
            this.decrementAttackCooldown();

            return this.attack_cooldown == 0 && !this.attack_is_finished && this.samurice.getAttackType() == Action.AttackType.SUMMON && this.samurice.getAttackAction() == Action.Attack.CLONES;
        }
    }
}

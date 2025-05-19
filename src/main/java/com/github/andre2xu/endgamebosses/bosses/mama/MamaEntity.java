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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

public class MamaEntity extends PathfinderMob implements GeoEntity {
    /*
    See 'CHANGE LATER' comments

    TODO:
    - Increase MAX_HEALTH attribute
    - Increase MAX_HEALTH attribute of egg sac
    - Increase MAX_HEALTH attribute of spiderlings
    - Implement spiderling damage
    - Increase damage dealt to target from charging
    - Increase damage dealt to target from leaping forward

    OPTIONAL:
    - Add boss music for Mama
    */



    // GENERAL
    private Long mama_id = null; // this is given to spiderlings so they know which Mama they belong to. The spiderlings increment/decrement Mama's child count so they need this id to find the correct Mama instance
    private final PartEntity<?>[] hitboxes;
    private Action.AttackType attack_type = Action.AttackType.MELEE; // this doesn't need to be synched between client and server so don't store it in an entity data accessor

    // BOSS FIGHT
    private final ServerBossEvent server_boss_event = new ServerBossEvent(
            Component.literal("Mama"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_12
    );
    private int boss_phase = 1;

    // DATA ACCESSORS
    private static final EntityDataAccessor<Integer> CHILD_COUNT = SynchedEntityData.defineId(MamaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_ACTION = SynchedEntityData.defineId(MamaEntity.class, EntityDataSerializers.INT); // actions need to be synched between client and server for animations

    // ACTIONS
    public enum Action {;
        // these determine which attack goal is run

        public enum AttackType {
            MELEE,
            RANGE
        }

        public enum Attack {
            NONE,

            // melee
            CHARGE,
            LEAP_FORWARD,

            // range
            WEB_SHOOT
        }
    }

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().then("animation.mama.walk", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation DEFENSIVE = RawAnimation.begin().then("animation.mama.defensive", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation DEFENSIVE_REVERSE = RawAnimation.begin().then("animation.mama.defensive_reverse", Animation.LoopType.HOLD_ON_LAST_FRAME);



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

        controllers.add(new AnimationController<>(this, "attack_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("defensive", DEFENSIVE)
                .triggerableAnim("defensive_reverse", DEFENSIVE_REVERSE)
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
        pBuilder.define(ATTACK_ACTION, 0); // none
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
    private void setAttackAction(Action.Attack attackAction) {
        int action_id = 0; // none

        switch (attackAction) {
            case Action.Attack.WEB_SHOOT:
                action_id = 1;
                this.attack_type = Action.AttackType.RANGE;
                break;
            case Action.Attack.CHARGE:
                action_id = 2;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.LEAP_FORWARD:
                action_id = 3;
                this.attack_type = Action.AttackType.MELEE;
                break;
            default:
                this.attack_type = Action.AttackType.MELEE;
        }

        this.entityData.set(ATTACK_ACTION, action_id);
    }

    private Action.Attack getAttackAction() {
        Action.Attack attack_action = Action.Attack.NONE;

        int action_id = this.entityData.get(ATTACK_ACTION);

        switch (action_id) {
            case 1:
                attack_action = Action.Attack.WEB_SHOOT;
                break;
            case 2:
                attack_action = Action.Attack.CHARGE;
                break;
            case 3:
                attack_action = Action.Attack.LEAP_FORWARD;
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
    public void makeStuckInBlock(@NotNull BlockState pState, @NotNull Vec3 pMotionMultiplier) {
        if (pState.is(Blocks.COBWEB)) {
            // don't get stuck in cobwebs
            return;
        }

        super.makeStuckInBlock(pState, pMotionMultiplier);
    }

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

        /*
        HOW ATTACKING WORKS:
        - There are two types: MELEE and RANGE (see Action.AttackType enums)
        - All attack goals have Minecraft's 'TARGET' flag set which means they will conflict with the target selector goals. The priority of 1 means they will be executed instead of a target selector goal
        - Only one attack goal can run at a time so it doesn't matter that they all share the same priority number. The priority's only purpose is to stop the target selector goals when an attack goal is run
        - To determine which attack goal is run, their 'canUse' methods check which Action enums are active. These enums are set/replaced in the aiStep method
        */
        this.goalSelector.addGoal(1, new WebShootAttackGoal(this));
        this.goalSelector.addGoal(1, new ChargeAttackGoal(this));
        this.goalSelector.addGoal(1, new LeapForwardAttackGoal(this));
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
            if (!target.isFallFlying()) {
                boolean is_attacking = this.getAttackAction() != Action.Attack.NONE;

                if (!is_attacking) {
                    this.getLookControl().setLookAt(target); // rotate horizontally to face target

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

                    // choose attack
                    if (this.boss_phase == 1) {
                        boolean should_attack = new Random().nextInt(1, 5) == 1; // 1 in 4 chances to attack

                        if (should_attack) {
                            this.setAttackAction(Action.Attack.WEB_SHOOT);
                        }
                    }
                    else if (this.boss_phase == 2) {
                        boolean should_attack = new Random().nextInt(1, 11) == 1; // 1 in 10 chances to attack

                        if (should_attack) {
                            int attack_choice = new Random().nextInt(1, 3);

                            switch (attack_choice) {
                                case 1:
                                    this.setAttackAction(Action.Attack.CHARGE);
                                    break;
                                case 2:
                                    this.setAttackAction(Action.Attack.LEAP_FORWARD);
                                default:
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

    private static class WebShootAttackGoal extends Goal {
        private final MamaEntity mama;
        private LivingEntity target = null;
        private Vec3 target_pos = null;
        private int attack_delay = 0; // delay is set in the 'start' method
        private int attack_cooldown = 0; // first attack has no cooldown
        private boolean attack_is_finished = false;

        public WebShootAttackGoal(MamaEntity mama) {
            this.mama = mama;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void turnAround() {
            if (this.target != null) {
                // calculate turn angle needed to make abdomen face the target
                double yaw_dx = this.target.getX() - this.mama.getX();
                double yaw_dz = this.target.getZ() - this.mama.getZ();

                float yaw_angle_towards_target = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians. This formula is: θ = Tan^-1(opp/adj)
                float turn_angle = (float) Math.toDegrees(-yaw_angle_towards_target) + 180;

                // turn around
                this.mama.setYRot(turn_angle);
                this.mama.setYBodyRot(turn_angle);
                this.mama.setYHeadRot(turn_angle);

                this.mama.triggerAnim("movement_trigger_anim_controller", "walk");
            }
        }

        private void generateCobwebsAroundTarget() {
            if (this.target_pos != null && this.mama.level() instanceof ServerLevel server_level) {
                BlockPos center = new BlockPos((int) this.target_pos.x, (int) this.target_pos.y, (int) this.target_pos.z);

                int radius = 2;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -3; dy <= 0; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            BlockPos current_block_pos = center.offset(dx, dy, dz);
                            BlockState current_block_state = server_level.getBlockState(current_block_pos);

                            boolean block_above_is_air = server_level.getBlockState(current_block_pos.above()).isAir();

                            if (!current_block_state.isAir() && !current_block_state.is(Blocks.COBWEB) && block_above_is_air) {
                                server_level.setBlockAndUpdate(current_block_pos.above(), Blocks.COBWEB.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.target = null;
            this.target_pos = null;
            this.attack_delay = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mama.getTarget' which can sometimes return null
            this.target = this.mama.getTarget();

            // set delay for attack to let Mama turn around first
            this.attack_delay = 20 * 2; // 2 seconds

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mama.setAttackAction(Action.Attack.NONE); // allow Mama to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                // OBJECTIVE: Turn around and spawn cobwebs around the target's position

                if (this.attack_cooldown > 0) {
                    // look at target while attack cooldown is counting down

                    this.mama.getLookControl().setLookAt(this.target);

                    this.attack_cooldown--;
                }
                else {
                    if (this.attack_delay > 0) {
                        if (this.attack_delay > 10) {
                            this.turnAround();
                        }
                        else {
                            // save the target's position when there's 0.5 seconds left. This is done to give them a chance to dodge
                            this.target_pos = this.target.position();
                        }

                        this.attack_delay--;
                    }
                    else {
                        // spawn cobwebs around the target's last known location
                        this.generateCobwebsAroundTarget();

                        this.mama.playSound(SoundEvents.SLIME_SQUISH, 2f, 1f);

                        // stop attack
                        this.attack_is_finished = true;

                        // set cooldown
                        this.attack_cooldown = 20 * 2; // 2 seconds
                    }
                }
            }
            else {
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.mama.getAttackType() == Action.AttackType.RANGE && this.mama.getAttackAction() == Action.Attack.WEB_SHOOT;
        }
    }

    private static class ChargeAttackGoal extends Goal {
        private final MamaEntity mama;
        private LivingEntity target = null;
        private int defensive_pose_duration = 0;
        private boolean is_in_defensive_pose = false;
        private final float attack_damage = 1f; // CHANGE LATER
        private int attack_duration = 0;
        private boolean attack_is_finished = false;

        public ChargeAttackGoal(MamaEntity mama) {
            this.mama = mama;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.target = null;
            this.defensive_pose_duration = 0;
            this.attack_duration = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mama.getTarget' which can sometimes return null
            this.target = this.mama.getTarget();

            // set defensive pose duration
            this.defensive_pose_duration = 20 * 3; // 2 seconds for the pose, 1 second to end the pose

            // set attack duration
            this.attack_duration = 20 * new Random().nextInt(3, 5); // 3 to 4 seconds

            super.start();
        }

        @Override
        public void stop() {
            if (this.is_in_defensive_pose) {
                this.mama.triggerAnim("attack_trigger_anim_controller", "defensive_reverse");
                this.is_in_defensive_pose = false;
            }

            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mama.setAttackAction(Action.Attack.NONE); // allow Mama to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                // face target
                this.mama.getLookControl().setLookAt(this.target);

                if (this.defensive_pose_duration > 0) {
                    // warn target of attack

                    if (this.defensive_pose_duration > 10) {
                        this.mama.triggerAnim("attack_trigger_anim_controller", "defensive");
                        this.is_in_defensive_pose = true;
                    }
                    else {
                        this.mama.triggerAnim("attack_trigger_anim_controller", "defensive_reverse");
                        this.is_in_defensive_pose = false;
                    }

                    this.defensive_pose_duration--;
                }
                else {
                    if (this.attack_duration > 0) {
                        // follow target and get close to them
                        Vec3 vector_to_target = this.target.position().subtract(this.mama.position());

                        if (this.mama.distanceTo(this.target) > 8) {
                            this.mama.setDeltaMovement(vector_to_target.normalize().scale(1.1));

                            this.mama.triggerAnim("movement_trigger_anim_controller", "walk");
                        }
                        else {
                            // damage target upon reaching them
                            this.target.hurt(this.mama.damageSources().mobAttack(this.mama), this.attack_damage);
                        }

                        // decrease duration
                        this.attack_duration--;
                    }
                    else {
                        // stop attack
                        this.attack_is_finished = true;
                    }
                }
            }
            else {
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.mama.getAttackType() == Action.AttackType.MELEE && this.mama.getAttackAction() == Action.Attack.CHARGE;
        }
    }

    private static class LeapForwardAttackGoal extends Goal {
        private final MamaEntity mama;
        private LivingEntity target = null;
        private Vec3 landing_pos = null;
        private double halfway_distance_to_landing_pos = 0;
        private int defensive_pose_duration = 0;
        private boolean is_in_defensive_pose = false;
        private final float attack_damage = 1f; // CHANGE LATER
        private boolean attack_is_finished = false;

        public LeapForwardAttackGoal(MamaEntity mama) {
            this.mama = mama;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && this.mama.distanceTo(this.target) <= 40 && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.target = null;
            this.landing_pos = null;
            this.halfway_distance_to_landing_pos = 0;
            this.defensive_pose_duration = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mama.getTarget' which can sometimes return null
            this.target = this.mama.getTarget();

            // set defensive pose duration
            this.defensive_pose_duration = 20 * 3; // 2 seconds for the pose, 1 second to end the pose

            super.start();
        }

        @Override
        public void stop() {
            if (this.is_in_defensive_pose) {
                this.mama.triggerAnim("attack_trigger_anim_controller", "defensive_reverse");
                this.is_in_defensive_pose = false;
            }

            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mama.setAttackAction(Action.Attack.NONE); // allow Mama to follow target & make attack decisions again

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                if (this.defensive_pose_duration > 0) {
                    // warn target of attack
                    this.mama.getLookControl().setLookAt(this.target);

                    if (this.defensive_pose_duration > 10) {
                        this.mama.triggerAnim("attack_trigger_anim_controller", "defensive");
                        this.is_in_defensive_pose = true;
                    }
                    else {
                        this.mama.triggerAnim("attack_trigger_anim_controller", "defensive_reverse");
                        this.is_in_defensive_pose = false;
                    }

                    // save target position a few seconds before leaping. This gives them time to dodge
                    if (this.defensive_pose_duration == 5) {
                        this.landing_pos = this.target.position();

                        this.halfway_distance_to_landing_pos = Math.sqrt(this.mama.distanceToSqr(this.landing_pos)) / 2;
                    }

                    this.defensive_pose_duration--;
                }
                else {
                    if (this.landing_pos != null) {
                        double current_distance_to_landing_spot = Math.sqrt(this.mama.distanceToSqr(this.landing_pos));

                        if (current_distance_to_landing_spot > 2) {
                            // OBJECTIVE: Jump N blocks in the air towards the landing spot. Fall back down once the halfway distance has been reached

                            Vec3 current_pos = this.mama.position();

                            double height = 0;

                            if (current_distance_to_landing_spot > this.halfway_distance_to_landing_pos) {
                                height = Math.floor(this.halfway_distance_to_landing_pos);
                            }

                            this.mama.setDeltaMovement(new Vec3(
                                    this.landing_pos.x - current_pos.x,
                                    (this.landing_pos.y + height) - current_pos.y,
                                    this.landing_pos.z - current_pos.z
                            ).normalize().scale(1.5)); // jump speed
                        }
                        else {
                            // stop attack
                            this.attack_is_finished = true;
                        }

                        // check if a collision occurred with the target during the leap
                        boolean has_collided_with_target = this.mama.getBoundingBox().intersects(this.target.getBoundingBox());

                        if (has_collided_with_target && this.mama.distanceTo(this.target) <= 6) {
                            // damage target
                            this.target.hurt(this.mama.damageSources().mobAttack(this.mama), this.attack_damage);
                        }
                    }
                    else {
                        // cancel attack
                        this.attack_is_finished = true;
                    }
                }
            }
            else {
                // cancel attack if target doesn't exist, is dead, is far away, or is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.mama.getAttackType() == Action.AttackType.MELEE && this.mama.getAttackAction() == Action.Attack.LEAP_FORWARD;
        }
    }
}

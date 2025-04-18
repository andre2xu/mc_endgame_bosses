package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import com.github.andre2xu.endgamebosses.bosses.ProjectilesRegistry;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.missile.MechalodonMissileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Predicate;

public class MechalodonEntity extends PathfinderMob implements GeoEntity {
    /*
    See 'CHANGE LATER' comments

    TODO:
    - Increase MAX_HEALTH attribute
    - Increase damage dealt to target from charging
    - Increase damage dealt to target from leaping forward
    - Increase damage dealt to target from biting
    - Increase damage dealt to target from the underground surprise attack
    - Increase damage dealt to target from the dive from above attack
    - Add sounds for Mechalodon
    - Implement attack for players using an elytra
    - Add particles for movement & attacks

    OPTIONAL:
    - Add a dodge mechanism for mace
    - Add boss music for Mechalodon
    */



    // GENERAL
    private Vec3 current_point_in_circle = new Vec3(0,0,0);
    private int angle_needed_to_find_next_circle_point;
    private final ArrayList<Integer> all_angles_needed_to_find_circle_points = new ArrayList<>();
    private Iterator<Integer> circle_point_angles_array_iterator;
    private Action.AttackType attack_type = Action.AttackType.MELEE; // this doesn't need to be synched between client and server so don't store it in an entity data accessor

    // BOSS FIGHT
    private final ServerBossEvent server_boss_event = new ServerBossEvent(
            Component.literal("Mechalodon"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_12
    );
    private int boss_phase = 1; // only needed on the server side. No need to save to persistent storage since it's tied to the boss health which is already saved

    // DATA ACCESSORS
    private static final EntityDataAccessor<Float> BODY_PITCH = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.FLOAT); // this is for adjusting the pitch of the Mechalodon's body in the model class
    private static final EntityDataAccessor<Integer> MOVE_ACTION = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.INT); // actions need to be synched between client and server for animations
    private static final EntityDataAccessor<Integer> ATTACK_ACTION = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.INT); // actions need to be synched between client and server for animations
    private static final EntityDataAccessor<Vector3f> ANCHOR_POINT = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.VECTOR3); // this is used for circling around the target. It is the target's position when the circling first starts

    // ACTIONS
    public enum Action {;
        // these determine which attack goal is run OR how the Mechalodon will move (see aiStep)

        public enum Move {
            IDLE,
            FOLLOW_TARGET,
            CIRCLE_AROUND_TARGET
        }

        public enum AttackType {
            MELEE,
            RANGE
        }

        public enum Attack {
            NONE,

            // melee
            CHARGE,
            LEAP_FORWARD,
            BITE,
            SURPRISE_FROM_BELOW,
            DIVE_FROM_ABOVE, // only in phase 2

            // range
            MISSILES // only in phase 2
        }
    }

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation SWIM_FAST_ANIM = RawAnimation.begin().then("animation.mechalodon.swim_fast", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation SWIM_SLOW_ANIM = RawAnimation.begin().then("animation.mechalodon.swim_slow", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation BITE_ANIM = RawAnimation.begin().then("animation.mechalodon.bite", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation MOUTH_OPEN_ANIM = RawAnimation.begin().then("animation.mechalodon.mouth_open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation MOUTH_CLOSE_ANIM = RawAnimation.begin().then("animation.mechalodon.mouth_close", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation SHOW_CANNON_ANIM = RawAnimation.begin().then("animation.mechalodon.show_cannon", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation HIDE_CANNON_ANIM = RawAnimation.begin().then("animation.mechalodon.hide_cannon", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation FACE_UP_ANIM = RawAnimation.begin().then("animation.mechalodon.face_up", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation FACE_DOWN_ANIM = RawAnimation.begin().then("animation.mechalodon.face_down", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation FACE_UP_REVERSE_ANIM = RawAnimation.begin().then("animation.mechalodon.face_up_reverse", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation FACE_DOWN_REVERSE_ANIM = RawAnimation.begin().then("animation.mechalodon.face_down_reverse", Animation.LoopType.HOLD_ON_LAST_FRAME);



    public MechalodonEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.noPhysics = true; // ignore block collisions
        this.noCulling = true; // stay rendered even when out of view

        // get all the angles needed to make the Mechalodon circle around a target
        int current_degree = 0;
        int degree_change = 45;
        int num_of_points = 360 / degree_change;

        for (int i = 0; i < num_of_points; i++) {
            this.all_angles_needed_to_find_circle_points.add(current_degree);
            current_degree += degree_change;
        }

        this.circle_point_angles_array_iterator = this.all_angles_needed_to_find_circle_points.iterator();
        this.angle_needed_to_find_next_circle_point = this.circle_point_angles_array_iterator.next(); // initialize with first value of circle point angles array

        // add custom controls
        this.lookControl = new MechalodonLookControl(this); // change the default look control
    }

    public static AttributeSupplier createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10) // CHANGE LATER
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // immune to knockback
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0) // immune to explosion knockback
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // add triggerable animations
        controllers.add(new AnimationController<>(this, "movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("swim_fast", SWIM_FAST_ANIM)
                .triggerableAnim("swim_slow", SWIM_SLOW_ANIM)
        );
        
        controllers.add(new AnimationController<>(this, "attack_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("bite", BITE_ANIM)
                .triggerableAnim("mouth_open", MOUTH_OPEN_ANIM)
                .triggerableAnim("mouth_close", MOUTH_CLOSE_ANIM)
                .triggerableAnim("show_cannon", SHOW_CANNON_ANIM)
                .triggerableAnim("hide_cannon", HIDE_CANNON_ANIM)
        );

        controllers.add(new AnimationController<>(this, "rotation_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("face_up", FACE_UP_ANIM)
                .triggerableAnim("face_down", FACE_DOWN_ANIM)
                .triggerableAnim("face_up_reverse", FACE_UP_REVERSE_ANIM)
                .triggerableAnim("face_down_reverse", FACE_DOWN_REVERSE_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geo_cache;
    }



    // DATA
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        super.defineSynchedData(pBuilder);

        // give data accessors starting values
        pBuilder.define(BODY_PITCH, 0.0f);
        pBuilder.define(MOVE_ACTION, 0); // idle
        pBuilder.define(ATTACK_ACTION, 0); // none
        pBuilder.define(ANCHOR_POINT, new Vector3f(0,0,0));
    }

    public float getBodyPitch() {
        return this.entityData.get(BODY_PITCH);
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
    private void setMoveAction(Action.Move moveAction) {
        int action_id = switch (moveAction) {
            case Action.Move.FOLLOW_TARGET -> 1;
            case Action.Move.CIRCLE_AROUND_TARGET -> 2;
            default -> 0; // idle
        };

        this.entityData.set(MOVE_ACTION, action_id);
    }

    private Action.Move getMoveAction() {
        int action_id = this.entityData.get(MOVE_ACTION);

        return switch (action_id) {
            case 1 -> Action.Move.FOLLOW_TARGET;
            case 2 -> Action.Move.CIRCLE_AROUND_TARGET;
            default -> Action.Move.IDLE;
        };
    }

    private void setAttackAction(Action.Attack attackAction) {
        int action_id = 0; // none

        switch (attackAction) {
            case Action.Attack.CHARGE:
                action_id = 1;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.LEAP_FORWARD:
                action_id = 2;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.BITE:
                action_id = 3;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.SURPRISE_FROM_BELOW:
                action_id = 4;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.DIVE_FROM_ABOVE:
                action_id = 5;
                this.attack_type = Action.AttackType.MELEE;
                break;
            case Action.Attack.MISSILES:
                action_id = 6;
                this.attack_type = Action.AttackType.RANGE;
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
                attack_action = Action.Attack.CHARGE;
                break;
            case 2:
                attack_action = Action.Attack.LEAP_FORWARD;
                break;
            case 3:
                attack_action = Action.Attack.BITE;
                break;
            case 4:
                attack_action = Action.Attack.SURPRISE_FROM_BELOW;
                break;
            case 5:
                attack_action = Action.Attack.DIVE_FROM_ABOVE;
                break;
            case 6:
                attack_action = Action.Attack.MISSILES;
                break;
            default:
        }

        return attack_action;
    }

    public Action.AttackType getAttackType() {
        return this.attack_type;
    }

    private Vec3 getNextPointOnCircle(double radius, double degree_change) {
        Vector3f anchor_point = this.entityData.get(ANCHOR_POINT);
        degree_change = Math.toRadians(degree_change);

        // get next point on circle
        double new_x = anchor_point.x + radius * Math.cos(degree_change);
        double new_y = anchor_point.y;
        double new_z = anchor_point.z + radius * Math.sin(degree_change);

        return new Vec3(new_x, new_y, new_z);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, @NotNull BlockState pState, @NotNull BlockPos pPos) {
        // taken from Minecraft's 'FlyingMob' class
    }

    @Override
    public boolean onClimbable() {
        // taken from Minecraft's 'FlyingMob' class

        return false;
    }

    @Override
    public void travel(@NotNull Vec3 pTravelVector) {
        // taken from Minecraft's 'FlyingMob' class. This ensures the movement works properly and doesn't overshoot

        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
            }
            else if (this.isInLava()) {
                this.moveRelative(0.02F, pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            }
            else {
                BlockPos ground = getBlockPosBelowThatAffectsMyMovement();
                float f = 0.91F;

                if (this.onGround()) {
                    f = this.level().getBlockState(ground).getFriction(this.level(), ground, this) * 0.91F;
                }

                float f1 = 0.16277137F / (f * f * f);
                f = 0.91F;

                if (this.onGround()) {
                    f = this.level().getBlockState(ground).getFriction(this.level(), ground, this) * 0.91F;
                }

                this.moveRelative(this.onGround() ? 0.1F * f1 : 0.02F, pTravelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(f));
            }
        }

        this.calculateEntityAnimation(false);
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt the Mechalodon
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
        this.goalSelector.addGoal(1, new ChargeAttackGoal(this));
        this.goalSelector.addGoal(1, new LeapForwardAttackGoal(this));
        this.goalSelector.addGoal(1, new BiteAttackGoal(this));
        this.goalSelector.addGoal(1, new SurpriseFromBelowAttackGoal(this));
        this.goalSelector.addGoal(1, new DiveFromAboveAttackGoal(this));
        this.goalSelector.addGoal(1, new MissilesAttackGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // update boss health bar
        float boss_health_remaining = this.getHealth() / this.getMaxHealth(); // in percentage
        this.server_boss_event.setProgress(boss_health_remaining);

        // update boss phase
        if (this.boss_phase == 1 && boss_health_remaining <= 0.5) {
            this.boss_phase = 2;
        }

        // handle movement
        LivingEntity target = this.getTarget();

        if (target != null) {
            if (!target.isFallFlying()) {
                Vec3 current_pos = this.position();
                Vec3 target_pos = target.position();

                boolean is_attacking = this.getAttackAction() != Action.Attack.NONE;

                if (!is_attacking) {
                    int allowed_distance_from_target = 20;
                    float distance_to_target = this.distanceTo(target);
                    Action.Move current_move_action = this.getMoveAction();

                    if (current_move_action != Action.Move.CIRCLE_AROUND_TARGET && distance_to_target > allowed_distance_from_target) {
                        // OBJECTIVE: Follow target until close enough to circle around them

                        this.setMoveAction(Action.Move.FOLLOW_TARGET);

                        // look at target
                        this.getLookControl().setLookAt(target);

                        // decide whether to attack or not while following. The attack involves movement towards the target as well so it's like an alternative way to follow the target
                        boolean should_attack = new Random().nextInt(1, 6) == 1; // 1 in 5 chances to attack

                        if (should_attack) {
                            this.setAttackAction(Action.Attack.CHARGE);
                        }
                        else {
                            // move to target
                            this.setDeltaMovement(this.getDeltaMovement().add(
                                    new Vec3(
                                            target_pos.x - current_pos.x,
                                            (target_pos.y - 2) - current_pos.y,
                                            target_pos.z - current_pos.z
                                    ).normalize().scale(0.5) // follow speed
                            ));

                            // run swim animation
                            this.triggerAnim("movement_trigger_anim_controller", "swim_fast");
                        }
                    }
                    else if (current_move_action == Action.Move.FOLLOW_TARGET && distance_to_target <= allowed_distance_from_target) {
                        // OBJECTIVE: Once the allowed distance has been reached, set the flag that allows the Mechalodon to circle around the target and save the target's position as an anchor point (it will be the circle's center)

                        this.setMoveAction(Action.Move.CIRCLE_AROUND_TARGET);

                        // save the player's position as the anchor point
                        Vector3f anchor_point = new Vector3f((float) target_pos.x, (float) target_pos.y, (float) target_pos.z);
                        this.entityData.set(ANCHOR_POINT, anchor_point);

                        // initialize the starting point on the circle
                        this.current_point_in_circle = this.position();

                        // verify that the circle is completely on solid ground. If not, cancel the circling and continue following the target
                        if (this.level() instanceof ServerLevel level) {
                            for (int angle : this.all_angles_needed_to_find_circle_points) {
                                Vec3 next_point = this.getNextPointOnCircle(
                                        this.current_point_in_circle.distanceTo(new Vec3(anchor_point.x, anchor_point.y, anchor_point.z)),
                                        angle
                                );

                                BlockPos block_pos = new BlockPos((int) next_point.x, (int) next_point.y, (int) next_point.z);
                                BlockPos block_pos_below1 = block_pos.below();
                                BlockPos block_pos_below2 = block_pos_below1.below();

                                // check if the block at the current circle point and the block below it are solid
                                if (level.getBlockState(block_pos).isAir() && level.getBlockState(block_pos_below1).isAir() && level.getBlockState(block_pos_below2).isAir()) {
                                    this.setMoveAction(Action.Move.FOLLOW_TARGET);
                                    break;
                                }
                            }
                        }
                    }

                    current_move_action = this.getMoveAction(); // update flag

                    if (current_move_action == Action.Move.CIRCLE_AROUND_TARGET) {
                        // OBJECTIVE: Check if the target is inside the circle (i.e. near anchor point) and move around them. Follow the target again if they leave the circle

                        Vector3f ap = this.entityData.get(ANCHOR_POINT);
                        Vec3 anchor_point = new Vec3(ap.x, ap.y, ap.z);

                        if (Math.sqrt(target.distanceToSqr(anchor_point)) < allowed_distance_from_target) {
                            // find the next point on the circle
                            Vec3 next_point = this.getNextPointOnCircle(
                                    this.current_point_in_circle.distanceTo(anchor_point),
                                    this.angle_needed_to_find_next_circle_point
                            );

                            if (Math.sqrt(this.distanceToSqr(next_point)) > 10) {
                                // this block handles the movement of the Mechalodon to the next point on the circle

                                // look at point
                                this.getLookControl().setLookAt(next_point);

                                // move to point
                                this.setDeltaMovement(this.getDeltaMovement().add(
                                        new Vec3(
                                                next_point.x - this.getX(),
                                                (target_pos.y - 2) - this.getY(), // move underground
                                                next_point.z - this.getZ()
                                        ).normalize().scale(0.1) // circling speed
                                ));

                                // run swim animation
                                this.triggerAnim("movement_trigger_anim_controller", "swim_slow");
                            }
                            else {
                                // decide whether to attack or get the next point on the circle

                                boolean should_attack = new Random().nextInt(1,4) == 1; // 1 in 3 chances to attack

                                if (should_attack) {
                                    // choose an attack
                                    if (this.boss_phase == 2) {
                                        int random_number = new Random().nextInt(1, 9); // pick a number from 1-8

                                        switch (random_number) {
                                            case 1:
                                                this.setAttackAction(Action.Attack.CHARGE);
                                                break;
                                            case 2:
                                                this.setAttackAction(Action.Attack.LEAP_FORWARD);
                                                break;
                                            case 3:
                                                this.setAttackAction(Action.Attack.SURPRISE_FROM_BELOW);
                                                break;
                                            case 4:
                                            case 5:
                                                this.setAttackAction(Action.Attack.DIVE_FROM_ABOVE);
                                                break;
                                            case 6:
                                            case 7:
                                            case 8:
                                                this.setAttackAction(Action.Attack.MISSILES);
                                                break;
                                            default:
                                        }
                                    }
                                    else {
                                        int random_number = new Random().nextInt(1, 4); // pick a number from 1-3

                                        switch (random_number) {
                                            case 1:
                                                this.setAttackAction(Action.Attack.CHARGE);
                                                break;
                                            case 2:
                                                this.setAttackAction(Action.Attack.LEAP_FORWARD);
                                                break;
                                            case 3:
                                                this.setAttackAction(Action.Attack.SURPRISE_FROM_BELOW);
                                                break;
                                            default:
                                        }
                                    }

                                    // update the angle needed for the next point so that the next point will roughly be where the Mechalodon is after the chosen attack (i.e. behind the player)
                                    int current_angle = this.angle_needed_to_find_next_circle_point;
                                    int new_angle = (current_angle + 180) + 45; // NOTE: 45 is the change in degrees per point (see constructor)

                                    this.circle_point_angles_array_iterator = this.all_angles_needed_to_find_circle_points.iterator(); // refresh the iterator so that it points to the beginning of the array

                                    int iterator_angle = this.circle_point_angles_array_iterator.next();

                                    while (this.circle_point_angles_array_iterator.hasNext() && iterator_angle != new_angle) {
                                        iterator_angle = this.circle_point_angles_array_iterator.next();
                                    }

                                    this.angle_needed_to_find_next_circle_point = iterator_angle;
                                }
                                else {
                                    // get the angle used for calculating the next point on the circle

                                    if (!this.circle_point_angles_array_iterator.hasNext()) {
                                        // get a new iterator to restart the circling
                                        this.circle_point_angles_array_iterator = this.all_angles_needed_to_find_circle_points.iterator();
                                    }

                                    this.angle_needed_to_find_next_circle_point = this.circle_point_angles_array_iterator.next(); // get current angle needed and switch to the next
                                }
                            }
                        }
                        else {
                            // change movement flag to follow target
                            this.setMoveAction(Action.Move.FOLLOW_TARGET);
                        }
                    }
                    else {
                        // OBJECTIVE: If circling around isn't possible, attack the target in other ways

                        if (this.boss_phase == 1) {
                            int random_number = new Random().nextInt(1, 7); // pick a number from 1-6

                            if (distance_to_target >= 10) {
                                // choose a melee attack that doesn't require the Mechalodon to get close
                                boolean perform_charge_attack = Set.of(1,2,3).contains(random_number); // 3/6 chance to do this
                                boolean perform_leap_forward_attack = Set.of(4,5).contains(random_number); // 2/6 chance to do this

                                if (perform_charge_attack) {
                                    this.setAttackAction(Action.Attack.CHARGE);
                                }
                                else if (perform_leap_forward_attack) {
                                    this.setAttackAction(Action.Attack.LEAP_FORWARD);
                                }
                            }
                            else {
                                // bite target if they get too close
                                this.setAttackAction(Action.Attack.BITE);
                            }
                        }
                        else if (this.boss_phase == 2) {
                            int random_number = new Random().nextInt(1, 14); // pick a number from 1-13

                            if (distance_to_target >= 10) {
                                // choose a melee attack that doesn't require the Mechalodon to get close
                                boolean perform_charge_attack = Set.of(1,2).contains(random_number); // 2/13 chance to do this
                                boolean perform_leap_forward_attack = Set.of(3,4).contains(random_number); // 2/13 chance to do this
                                boolean perform_homing_missiles_attack = Set.of(5,6,7,8,9).contains(random_number); // 5/13 chance to do this
                                boolean perform_dive_from_above_attack = Set.of(10,11,12,13).contains(random_number); // 4/13 chance to do this

                                if (perform_charge_attack) {
                                    this.setAttackAction(Action.Attack.CHARGE);
                                }
                                else if (perform_leap_forward_attack) {
                                    this.setAttackAction(Action.Attack.LEAP_FORWARD);
                                }
                                else if (perform_homing_missiles_attack) {
                                    this.setAttackAction(Action.Attack.MISSILES);
                                }
                                else if (perform_dive_from_above_attack) {
                                    this.setAttackAction(Action.Attack.DIVE_FROM_ABOVE);
                                }
                            }
                            else {
                                // bite target if they get too close
                                this.setAttackAction(Action.Attack.BITE);
                            }
                        }
                    }
                }
            }
        }
    }



    // CONTROLS
    private static class MechalodonLookControl extends LookControl {
        public MechalodonLookControl(Mob pMob) {
            super(pMob);
        }

        @Override
        public void setLookAt(@NotNull Entity pEntity) {
            super.setLookAt(pEntity); // update value of 'this.mob.getXRot'

            Vec3 target_pos = pEntity.position();

            // set yaw to face target
            double yaw_dx = target_pos.x - this.mob.getX();
            double yaw_dz = target_pos.z - this.mob.getZ();

            float yaw_angle_towards_target = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians. This formula is: θ = Tan^-1(opp/adj)
            float new_yaw = (float) Math.toDegrees(-yaw_angle_towards_target);

            this.mob.setYRot(new_yaw);
            this.mob.setYBodyRot(new_yaw);
            this.mob.setYHeadRot(new_yaw);

            // set pitch to face target
            float new_pitch = this.mob.getXRot();

            if (new_pitch > 0) {
                float pitch_adjustment = 0.2f;

                this.mob.getEntityData().set(BODY_PITCH, (float) -Math.toRadians(new_pitch) + pitch_adjustment); // GeckoLib uses radians. Rotation is done in the 'setCustomAnimations' method of the model class
            }
        }

        @Override
        public void setLookAt(@NotNull Vec3 pLookVector) {
            super.setLookAt(pLookVector);

            // set yaw to face look vector
            double yaw_dx = pLookVector.x - this.mob.getX();
            double yaw_dz = pLookVector.z - this.mob.getZ();

            float yaw_angle_towards_look_vector = (float) Mth.atan2(yaw_dx, yaw_dz); // angle is in radians. This formula is: θ = Tan^-1(opp/adj)
            float new_yaw = (float) Math.toDegrees(-yaw_angle_towards_look_vector);

            this.mob.setYRot(new_yaw);
            this.mob.setYBodyRot(new_yaw);
            this.mob.setYHeadRot(new_yaw);

            // set pitch to default (i.e. body is straight)
            this.mob.getEntityData().set(BODY_PITCH, (float) -Math.toRadians(0)); // GeckoLib uses radians. Rotation is done in the 'setCustomAnimations' method of the model class
        }
    }



    // CUSTOM GOALS
    private static class SelectTargetGoal extends NearestAttackableTargetGoal<Player> {
        public SelectTargetGoal(Mob pMob) {
            // this is a custom constructor made to reduce the amount of parameters. It doesn't override any constructor from the parent

            this(pMob, Player.class, 10, true, false, null);
        }

        public SelectTargetGoal(Mob pMob, Class<Player> pTargetType, int pRandomInterval, boolean pMustSee, boolean pMustReach, @Nullable Predicate<LivingEntity> pTargetPredicate) {
            // this is the main constructor where the target conditions are set (see NearestAttackableTargetGoal). It was overridden to increase how far the Mechalodon can spot targets

            super(pMob, pTargetType, pRandomInterval, pMustSee, pMustReach, pTargetPredicate);

            final double MAX_TARGET_DISTANCE = 50d; // blocks
            this.targetConditions = TargetingConditions
                    .forCombat()
                    .ignoreLineOfSight() // ensures target can be seen even through blocks (this is needed since Mechalodon can move underground)
                    .range(MAX_TARGET_DISTANCE)
                    .selector(pTargetPredicate);
        }
    }

    private static class ChargeAttackGoal extends Goal {
        private final MechalodonEntity mechalodon;
        private LivingEntity target = null;
        private final float attack_damage = 1f; // CHANGE LATER
        private int attack_duration; // see resetAttack
        private boolean attack_is_finished = false;

        public ChargeAttackGoal(MechalodonEntity mechalodon) {
            this.mechalodon = mechalodon;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void decrementAttackDuration() {
            this.attack_duration--;
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.attack_duration = 20 * 6; // 6 seconds
            this.attack_is_finished = false;
            this.target = null;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mechalodon.getTarget' which can sometimes return null
            this.target = this.mechalodon.getTarget();

            if (this.canAttack()) {
                // look at target
                this.mechalodon.getLookControl().setLookAt(this.target);
            }

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mechalodon.setAttackAction(Action.Attack.NONE); // allow the Mechalodon's aiStep movement to run again

            super.stop();
        }

        @Override
        public void tick() {
            // OBJECTIVE: Move towards the target really quickly and try to collide with them before the attack duration drops to zero. If the collision is successful, deal damage to the target

            if (this.attack_duration == 0) {
                this.attack_is_finished = true;
            }
            else {
                if (this.canAttack()) {
                    // move towards target
                    Vec3 current_pos = this.mechalodon.position();
                    Vec3 target_pos = target.position();

                    this.mechalodon.setDeltaMovement(this.mechalodon.getDeltaMovement().add(
                            new Vec3(
                                    target_pos.x - current_pos.x,
                                    (target_pos.y - 2) - current_pos.y,
                                    target_pos.z - current_pos.z
                            ).normalize().scale(1) // follow speed
                    ));

                    // run swim animation
                    this.mechalodon.triggerAnim("movement_trigger_anim_controller", "swim_fast");

                    // check if collision occurred
                    boolean has_collided_with_target = this.mechalodon.getBoundingBox().intersects(target.getBoundingBox());

                    if (has_collided_with_target) {
                        // damage target
                        this.target.hurt(this.mechalodon.damageSources().mobAttack(this.mechalodon), this.attack_damage);

                        // stop attack
                        this.attack_is_finished = true;
                    }
                }
                else {
                    // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode
                    this.attack_is_finished = true;
                }

                this.decrementAttackDuration();
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.mechalodon.getAttackType() == Action.AttackType.MELEE && this.mechalodon.getAttackAction() == Action.Attack.CHARGE;
        }
    }

    private static class LeapForwardAttackGoal extends Goal {
        private final MechalodonEntity mechalodon;
        private LivingEntity target = null;
        private Vec3 landing_position = null;
        private float leap_highest_point;
        private final float attack_damage = 1f; // CHANGE LATER
        private boolean attack_is_finished = false;

        public LeapForwardAttackGoal(MechalodonEntity mechalodon) {
            this.mechalodon = mechalodon;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private void resetAttack() {
            this.attack_is_finished = false;
            this.target = null;
            this.landing_position = null;
            this.leap_highest_point = 0;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mechalodon.getTarget' which can sometimes return null
            this.target = this.mechalodon.getTarget();

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mechalodon.setAttackAction(Action.Attack.NONE); // allow the Mechalodon's aiStep movement to run again

            super.stop();
        }

        @Override
        public void tick() {
            // OBJECTIVE: Leap towards the target's saved position (the one at the start of the attack) and land directly behind. If a collision occurred with the target during the leap, deal damage to them

            if (this.target != null && this.target.isAlive()) {
                if (this.target instanceof Player player && (player.isCreative() || player.isSpectator())) {
                    // stop attack if target is a player in creative/spectator mode
                    this.attack_is_finished = true;

                    // close mouth if it was opened
                    if (this.landing_position != null) {
                        this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_close");
                    }

                    return;
                }

                Vec3 current_pos = this.mechalodon.position();
                Vec3 target_pos = this.target.position();

                // get landing position
                if (this.landing_position == null) {
                    // look at target
                    this.mechalodon.getLookControl().setLookAt(this.target);

                    // calculate landing position
                    Vec3 vector_towards_target = new Vec3(
                            target_pos.x - current_pos.x,
                            (target_pos.y - 2) - current_pos.y,
                            target_pos.z - current_pos.z
                    ).normalize(); // normalize reduces the vector distance to 1 block

                    double xz_scale = 18; // leap distance
                    this.landing_position = target_pos.add(vector_towards_target.multiply(xz_scale, 1, xz_scale)); // towards target but "behind" them (i.e. extend the vector that points towards the target)

                    // get the point in the leap where the y-position of the Mechalodon should be at its highest
                    double starting_distance_to_landing_point = Math.sqrt(this.mechalodon.distanceToSqr(this.landing_position));
                    this.leap_highest_point = (float) starting_distance_to_landing_point * 0.4f;

                    // run animations (these are only played at the start of the leap since this entire block is only executed at that time)
                    this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_open");
                    this.mechalodon.triggerAnim("movement_trigger_anim_controller", "swim_fast");
                }

                // leap towards landing position
                double distance_to_landing_pos = Math.sqrt(this.mechalodon.distanceToSqr(this.landing_position));

                if (distance_to_landing_pos > this.leap_highest_point) {
                    // OBJECTIVE: Move up to the highest point

                    this.mechalodon.setDeltaMovement(new Vec3(
                            this.landing_position.x - current_pos.x,
                            (target_pos.y + 4) - current_pos.y,
                            this.landing_position.z - current_pos.z
                    ).normalize().scale(0.9)); // jump speed
                }
                else {
                    // OBJECTIVE: Move down to the landing position

                    this.mechalodon.setDeltaMovement(new Vec3(
                            this.landing_position.x - current_pos.x,
                            this.landing_position.y - current_pos.y,
                            this.landing_position.z - current_pos.z
                    ).normalize().scale(0.7)); // fall speed
                }

                // check for collision with target while leaping
                boolean has_collided_with_target = this.mechalodon.getBoundingBox().intersects(target.getBoundingBox());

                if (has_collided_with_target) {
                    // damage target
                    this.target.hurt(this.mechalodon.damageSources().mobAttack(this.mechalodon), this.attack_damage);

                    // close mouth
                    this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_close");
                }

                // check if landing position has been reached and stop attack
                if (distance_to_landing_pos <= 2) {
                    this.attack_is_finished = true;

                    // close mouth if it's still open
                    if (!has_collided_with_target) {
                        this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_close");
                    }
                }
            }
            else {
                // stop attack if target doesn't exist or is dead
                this.attack_is_finished = true;

                // close mouth
                this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_close");
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.mechalodon.getAttackType() == Action.AttackType.MELEE && this.mechalodon.getAttackAction() == Action.Attack.LEAP_FORWARD;
        }
    }

    private static class BiteAttackGoal extends Goal {
        private final MechalodonEntity mechalodon;
        private LivingEntity target = null;
        private Vec3 target_pos = null;
        private final float attack_damage = 1f; // CHANGE LATER
        private float attack_cooldown = 0; // no cooldown for the first bite
        private boolean attack_is_finished = false;

        public BiteAttackGoal(MechalodonEntity mechalodon) {
            this.mechalodon = mechalodon;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void decrementCooldown() {
            // this runs whenever the 'canUse' method is called

            if (this.attack_cooldown > 0) {
                this.attack_cooldown--;
            }
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.attack_is_finished = false;
            this.target = null;
            this.target_pos = null;
            this.attack_cooldown = 20f; // 1 second
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mechalodon.getTarget' which can sometimes return null
            this.target = this.mechalodon.getTarget();

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mechalodon.setAttackAction(Action.Attack.NONE); // allow the Mechalodon's aiStep movement to run again

            super.stop();
        }

        @Override
        public void tick() {
            // OBJECTIVE: Move towards the target's saved position (the one at the start of the attack) and deal damage to the target if a collision occurred while doing so

            if (this.canAttack()) {
                // save target position before going in for the bite. This is done so the target isn't followed which allows them to dodge
                Vec3 target_pos = this.target.position();

                if (this.target_pos == null) {
                    if (Math.sqrt(this.mechalodon.distanceToSqr(target_pos)) <= 10) {
                        this.target_pos = target_pos.subtract(0, 1, 0); // towards target but 1 block below them

                        // look at saved target position
                        this.mechalodon.getLookControl().setLookAt(this.target_pos);
                    }
                    else {
                        // cancel attack since target was far away
                        this.attack_is_finished = true;

                        return;
                    }
                }

                // move to saved target position
                Vec3 current_pos = this.mechalodon.position();

                this.mechalodon.setDeltaMovement(new Vec3(
                        this.target_pos.x - current_pos.x,
                        this.target_pos.y - current_pos.y,
                        this.target_pos.z - current_pos.z
                ).normalize().scale(0.3)); // movement speed

                // check if collision occurred
                boolean has_collided_with_target = this.mechalodon.getBoundingBox().intersects(target.getBoundingBox());

                if (has_collided_with_target) {
                    // run bite animation
                    this.mechalodon.triggerAnim("attack_trigger_anim_controller", "bite");

                    // damage target
                    this.target.hurt(this.mechalodon.damageSources().mobAttack(this.mechalodon), this.attack_damage);

                    // stop attack
                    this.attack_is_finished = true;

                    return;
                }

                // check if saved target position has been reached and stop attack
                if (Math.sqrt(this.mechalodon.distanceToSqr(this.target_pos)) <= 1) {
                    this.attack_is_finished = true;
                }
            }
            else {
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            this.decrementCooldown();

            return !this.attack_is_finished && this.attack_cooldown == 0 && this.mechalodon.getAttackType() == Action.AttackType.MELEE && this.mechalodon.getAttackAction() == Action.Attack.BITE;
        }
    }

    private static class SurpriseFromBelowAttackGoal extends Goal {
        private final MechalodonEntity mechalodon;
        private LivingEntity target = null;
        private Vec3 target_pos = null;
        private boolean has_resurfaced = false;
        private float stop_attack_delay; // this delay makes the animation reset, which occurs after the attack, look smoother
        private final float attack_damage = 1f; // CHANGE LATER
        private float attack_countdown; // countdown for surprise attack (starts decrementing when Mechalodon is directly below target)
        private boolean attack_is_finished = false;

        public SurpriseFromBelowAttackGoal(MechalodonEntity mechalodon) {
            this.mechalodon = mechalodon;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void decrementStopAttackDelay() {
            if (this.stop_attack_delay > 0) {
                this.stop_attack_delay--;
            }
        }

        private void decrementAttackCountdown() {
            if (this.attack_countdown > 0) {
                this.attack_countdown--;
            }
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.attack_is_finished = false;
            this.target = null;
            this.target_pos = null;
            this.has_resurfaced = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mechalodon.getTarget' which can sometimes return null
            this.target = this.mechalodon.getTarget();

            // reset countdown
            this.attack_countdown = 20 * 4; // 4 seconds

            // reset the delay
            this.stop_attack_delay = 10; // 0.5 seconds

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mechalodon.setAttackAction(Action.Attack.NONE); // allow the Mechalodon's aiStep movement to run again
            this.mechalodon.setMoveAction(Action.Move.FOLLOW_TARGET); // stop circling

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                Vec3 current_pos = this.mechalodon.position();
                Vec3 target_pos = this.target.position();

                // move to the target and stay below them until the countdown falls to zero
                if (this.attack_countdown > 0) {
                    this.mechalodon.setDeltaMovement(new Vec3(
                            target_pos.x - current_pos.x,
                            (target_pos.y - 10) - current_pos.y,
                            target_pos.z - current_pos.z
                    ).normalize().scale(0.8)); // movement speed

                    this.decrementAttackCountdown();
                }
                else {
                    // check if the countdown has finished and save the target's last position
                    if (this.target_pos == null) {
                        this.target_pos = target_pos;
                    }

                    // once the target's last position has been saved, launch surprise attack
                    if (!this.has_resurfaced) {
                        // OBJECTIVE: Stop moving and do surprise attack (doesn't matter if the target has moved or not)

                        this.mechalodon.triggerAnim("rotation_trigger_anim_controller", "face_up");
                        this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_open");

                        this.mechalodon.setDeltaMovement(new Vec3(
                                this.target_pos.x - current_pos.x,
                                this.target_pos.y - current_pos.y,
                                this.target_pos.z - current_pos.z
                        ).normalize().scale(1)); // movement speed

                        // check if collision occurred
                        boolean has_collided_with_target = this.mechalodon.getBoundingBox().intersects(target.getBoundingBox());

                        if (has_collided_with_target) {
                            this.target.hurt(this.mechalodon.damageSources().mobAttack(this.mechalodon), this.attack_damage);
                        }

                        // check if the last position of the target has been reached
                        if (this.mechalodon.position().y >= this.target_pos.y - 1) {
                            this.has_resurfaced = true;
                        }
                    }
                    else {
                        // stop attack after a delay
                        if (this.stop_attack_delay > 0) {
                            this.decrementStopAttackDelay();
                        }
                        else {
                            // reset animations
                            this.mechalodon.triggerAnim("rotation_trigger_anim_controller", "face_up_reverse");
                            this.mechalodon.triggerAnim("attack_trigger_anim_controller", "mouth_close");

                            this.attack_is_finished = true;
                        }
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
            return !this.attack_is_finished && this.mechalodon.getAttackType() == Action.AttackType.MELEE && this.mechalodon.getAttackAction() == Action.Attack.SURPRISE_FROM_BELOW;
        }
    }

    private static class DiveFromAboveAttackGoal extends Goal {
        private final MechalodonEntity mechalodon;
        private LivingEntity target = null;
        private Vec3 target_pos = null;
        private int attack_delay;
        private final float attack_damage = 1f; // CHANGE LATER
        private boolean attack_is_finished = false;

        public DiveFromAboveAttackGoal(MechalodonEntity mechalodon) {
            this.mechalodon = mechalodon;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void decrementAttackDelay() {
            if (this.attack_delay > 0) {
                this.attack_delay--;
            }
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.attack_is_finished = false;
            this.target = null;
            this.target_pos = null;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mechalodon.getTarget' which can sometimes return null
            this.target = this.mechalodon.getTarget();

            // reset attack delay
            this.attack_delay = 20; // 1 second

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            this.mechalodon.setAttackAction(Action.Attack.NONE); // allow the Mechalodon's aiStep movement to run again

            this.mechalodon.setMoveAction(Action.Move.FOLLOW_TARGET); // cancel circling if it's being done

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                // OBJECTIVE: Move above the target and save their position. Wait for 1 second and dive down really quickly towards the saved position. Deal damage to target if a collision occurred

                if (this.target_pos == null) {
                    Vec3 current_pos = this.mechalodon.position();
                    Vec3 target_pos = this.target.position();

                    int height_to_dive_down = 30;

                    // move towards target
                    this.mechalodon.setDeltaMovement(new Vec3(
                            target_pos.x - current_pos.x,
                            (target_pos.y + height_to_dive_down) - current_pos.y,
                            target_pos.z - current_pos.z
                    ).normalize().scale(0.8)); // movement speed

                    // update positions
                    current_pos = this.mechalodon.position();
                    target_pos = this.target.position();

                    // check if above target and save their position
                    if (Math.round(current_pos.x) == Math.round(target_pos.x) && Math.round(current_pos.z) == Math.round(target_pos.z) && Math.round(current_pos.y) >= Math.round(target_pos.y + height_to_dive_down)) {
                        this.target_pos = target_pos;
                    }
                }
                else {
                    if (this.attack_delay > 0) {
                        this.decrementAttackDelay();
                    }
                    else {
                        // rotate model to face downwards
                        this.mechalodon.triggerAnim("rotation_trigger_anim_controller", "face_down");

                        // dive down
                        Vec3 current_pos = this.mechalodon.position();

                        this.mechalodon.setDeltaMovement(new Vec3(
                                this.target_pos.x - current_pos.x,
                                this.target_pos.y - current_pos.y,
                                this.target_pos.z - current_pos.z
                        ).normalize().scale(1)); // dive speed

                        // check if collision occurred while diving
                        boolean has_collided_with_target = this.mechalodon.getBoundingBox().intersects(this.target.getBoundingBox());

                        if (has_collided_with_target) {
                            this.target.hurt(this.mechalodon.damageSources().mobAttack(this.mechalodon), this.attack_damage);
                        }

                        // check if the saved target position has been reached
                        if (Math.sqrt(this.mechalodon.distanceToSqr(this.target_pos)) <= 2) {
                            // straighten model again
                            this.mechalodon.triggerAnim("rotation_trigger_anim_controller", "face_down_reverse");

                            // stop the attack
                            this.attack_is_finished = true;
                        }
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
            return !this.attack_is_finished && this.mechalodon.getAttackType() == Action.AttackType.MELEE && this.mechalodon.getAttackAction() == Action.Attack.DIVE_FROM_ABOVE;
        }
    }

    private static class MissilesAttackGoal extends Goal {
        private final MechalodonEntity mechalodon;
        private LivingEntity target = null;
        private int wait_duration; // how long to wait for target to come out of hiding
        private int attack_cooldown = 0; // no cooldown for first attack
        private int attack_duration;
        private boolean attack_is_finished = false;

        public MissilesAttackGoal(MechalodonEntity mechalodon) {
            this.mechalodon = mechalodon;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
        }

        private void decrementWaitDuration() {
            if (this.wait_duration > 0) {
                this.wait_duration--;
            }
        }

        private void resetWaitDuration() {
            this.wait_duration = 30; // 1.5 seconds
        }

        private void decrementAttackCooldown() {
            if (this.attack_cooldown > 0) {
                this.attack_cooldown--;
            }
        }

        private void resetAttackCooldown() {
            this.attack_cooldown = 20 * 2; // 2 seconds
        }

        private void decrementAttackDuration() {
            if (this.attack_duration > 0) {
                this.attack_duration--;
            }
        }

        private void resetAttackDuration() {
            this.attack_duration = 20 * 8; // 8 seconds
        }

        private boolean noObstaclesInTheWay() {
            if (this.canAttack()) {
                Level level = this.mechalodon.level();

                // check if there are blocks in the way
                BlockHitResult block_hit_result = level.clip(new ClipContext(
                        this.mechalodon.getEyePosition(),
                        this.target.position(),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        this.mechalodon
                ));

                boolean no_obstacles = block_hit_result.getType() == HitResult.Type.MISS;
                boolean within_firing_range = this.mechalodon.distanceTo(this.target) <= 30;

                return no_obstacles && within_firing_range;
            }

            return false;
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        private void resetAttack() {
            this.attack_is_finished = false;
            this.target = null;
            this.attack_cooldown = 0;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.mechalodon.getTarget' which can sometimes return null
            this.target = this.mechalodon.getTarget();

            // show cannon
            this.mechalodon.triggerAnim("attack_trigger_anim_controller", "show_cannon");

            // initialize attack duration
            this.resetAttackDuration();

            super.start();
        }

        @Override
        public void stop() {
            this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

            // hide cannon
            this.mechalodon.triggerAnim("attack_trigger_anim_controller", "hide_cannon");

            // decide next course of action
            if (this.wait_duration == 0) {
                this.mechalodon.setAttackAction(Action.Attack.CHARGE); // charge towards target that's hiding
            }
            else {
                this.mechalodon.setAttackAction(Action.Attack.NONE); // allow the Mechalodon's aiStep movement to run again
            }

            // cancel circling if it's being done
            this.mechalodon.setMoveAction(Action.Move.FOLLOW_TARGET);

            super.stop();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                // OBJECTIVE: Check if the target is NOT hiding behind something and shoot them with a missile. If they are hiding, wait a few seconds for them to come out. If they're still hiding, cancel the attack and charge towards them (charging is triggered by the 'stop' method)

                if (this.attack_duration == 0) {
                    // stop attack
                    this.attack_is_finished = true;

                    return;
                }

                if (this.noObstaclesInTheWay()) {
                    // reset wait duration
                    this.resetWaitDuration();

                    // move close enough to target
                    Vec3 current_pos = this.mechalodon.position();
                    Vec3 target_pos = this.target.position();

                    double height_to_fire_at = target_pos.y + 10;

                    if (this.mechalodon.distanceTo(this.target) > 20 || current_pos.y < height_to_fire_at) {
                        this.mechalodon.setDeltaMovement(new Vec3(
                                target_pos.x - current_pos.x,
                                height_to_fire_at - current_pos.y,
                                target_pos.z - current_pos.z
                        ).normalize().scale(0.6)); // flight speed
                    }

                    // look at target
                    this.mechalodon.getLookControl().setLookAt(this.target);

                    // shoot missile
                    if (this.attack_cooldown == 0) {
                        Level level = this.mechalodon.level();

                        if (!level.isClientSide) {
                            MechalodonMissileEntity missile = ProjectilesRegistry.MECHALODON_MISSILE.get().create(level);

                            if (missile != null) {
                                missile.setPos(current_pos);
                                missile.setTarget(this.target);

                                level.addFreshEntity(missile);
                            }
                        }

                        // restart cooldown
                        this.resetAttackCooldown();
                    }
                }
                else {
                    this.decrementWaitDuration();

                    if (this.wait_duration == 0) {
                        // cancel attack
                        this.attack_is_finished = true;
                    }
                }

                // decrease attack cooldown
                this.decrementAttackCooldown();

                // decrease attack duration
                this.decrementAttackDuration();
            }
            else {
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.mechalodon.getAttackType() == Action.AttackType.RANGE && this.mechalodon.getAttackAction() == Action.Attack.MISSILES;
        }
    }
}

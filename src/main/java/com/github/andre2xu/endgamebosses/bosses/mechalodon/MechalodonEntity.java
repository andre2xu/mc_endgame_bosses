package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public class MechalodonEntity extends FlyingMob implements GeoEntity {
    // GENERAL
    private Vec3 current_point_in_circle = new Vec3(0,0,0);
    private int angle_needed_to_find_next_circle_point;
    private final ArrayList<Integer> all_angles_needed_to_find_circle_points = new ArrayList<>();
    private Iterator<Integer> circle_point_angles_array_iterator;

    // DATA ACCESSORS
    private static final EntityDataAccessor<Float> BODY_PITCH = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.FLOAT); // this is for adjusting the pitch of the Mechalodon's body in the model class
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.INT); // actions need to be synched between client and server for animations
    private static final EntityDataAccessor<Vector3f> ANCHOR_POINT = SynchedEntityData.defineId(MechalodonEntity.class, EntityDataSerializers.VECTOR3); // this is used for circling around the target. It is the target's position when the circling first starts

    // ACTIONS
    private enum Action {;
        enum Move {
            IDLE,
            FOLLOW_TARGET,
            CIRCLE_AROUND_TARGET
        }
    }

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation SWIM_FAST_ANIM = RawAnimation.begin().then("animation.mechalodon.swim_fast", Animation.LoopType.PLAY_ONCE);



    public MechalodonEntity(EntityType<? extends FlyingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

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
                .build();
    }



    // GECKOLIB SETUP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // add triggerable animations
        controllers.add(new AnimationController<>(this, "swim_fast_anim_controller", state -> PlayState.STOP).triggerableAnim("swim_fast", SWIM_FAST_ANIM));
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
        pBuilder.define(ACTION, 0); // idle
        pBuilder.define(ANCHOR_POINT, new Vector3f(0,0,0));
    }

    public float getBodyPitch() {
        return this.entityData.get(BODY_PITCH);
    }



    // AI
    private void setMoveAction(Action.Move moveAction) {
        int action_id = switch (moveAction) {
            case Action.Move.FOLLOW_TARGET -> 1;
            case Action.Move.CIRCLE_AROUND_TARGET -> 2;
            default -> 0; // idle
        };

        this.entityData.set(ACTION, action_id);
    }

    private Action.Move getMoveAction() {
        int action_id = this.entityData.get(ACTION);

        return switch (action_id) {
            case 1 -> Action.Move.FOLLOW_TARGET;
            case 2 -> Action.Move.CIRCLE_AROUND_TARGET;
            default -> Action.Move.IDLE;
        };
    }

    private void resetAnchorPoint() {
        this.entityData.set(ANCHOR_POINT, new Vector3f(0,0,0));
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
    protected void registerGoals() {
        // find and select a target
        this.targetSelector.addGoal(1, new SelectTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.noPhysics = true; // ignore block collisions (only enabled in the first phase)

        LivingEntity target = this.getTarget();

        if (target != null) {
            if (target.onGround()) {
                Vec3 current_pos = this.position();
                Vec3 target_pos = target.position();

                int allowed_distance_from_target = 20;

                if (this.getMoveAction() != Action.Move.CIRCLE_AROUND_TARGET && this.distanceTo(target) > allowed_distance_from_target) {
                    // OBJECTIVE: Follow target until close enough to circle around them

                    this.setMoveAction(Action.Move.FOLLOW_TARGET);
                    this.resetAnchorPoint();

                    // look at target
                    this.getLookControl().setLookAt(target);

                    // move to target
                    this.setDeltaMovement(this.getDeltaMovement().add(
                            new Vec3(
                                    target_pos.x - current_pos.x,
                                    (target_pos.y - 2) - current_pos.y,
                                    target_pos.z - current_pos.z
                            ).normalize().scale(0.2) // follow speed
                    ));

                    // run swim animation
                    this.triggerAnim("swim_fast_anim_controller", "swim_fast");
                }
                else if (this.getMoveAction() == Action.Move.FOLLOW_TARGET) {
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

                if (this.getMoveAction() == Action.Move.CIRCLE_AROUND_TARGET) {
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
                                    ).normalize().scale(0.18) // circling speed
                            ));

                            // run swim animation
                            this.triggerAnim("swim_fast_anim_controller", "swim_fast");
                        }
                        else {
                            // this block changes the angle used for calculating the next point on the circle

                            if (!this.circle_point_angles_array_iterator.hasNext()) {
                                // get a new iterator to restart the circling
                                this.circle_point_angles_array_iterator = this.all_angles_needed_to_find_circle_points.iterator();
                            }

                            this.angle_needed_to_find_next_circle_point = this.circle_point_angles_array_iterator.next(); // get current angle needed and switch to the next
                        }
                    }
                    else {
                        // change movement flag to follow target
                        this.setMoveAction(Action.Move.FOLLOW_TARGET);
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
}

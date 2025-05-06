package com.github.andre2xu.endgamebosses.bosses.tragon;

import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.FireHead;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.IceHead;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.LightningHead;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.TragonHead;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fluids.FluidType;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Predicate;

public class TragonEntity extends PathfinderMob implements GeoEntity {
    /*
    See 'CHANGE LATER' comments

    TODO:
    - Increase MAX_HEALTH attribute
    - Increase damage dealt to target from frost breath
    - Increase damage dealt to target from fire breath
    - Increase damage dealt to target from laser beam
    - Make damages inflicted to targets underwater less than that of land

    OPTIONAL:
    - Add boss music for Tragon
    */



    // GENERAL
    private PartEntity<?>[] hitboxes;
    private final String fire_head_neck_id = "fire_head_neck";
    private final String lightning_head_neck_id = "lightning_head_neck";
    private final String ice_head_neck_id = "ice_head_neck";
    private final HashMap<String, TragonHead> heads = new HashMap<>();
    private TragonEntity.Action.AttackType attack_type = TragonEntity.Action.AttackType.MELEE; // this doesn't need to be synched between client and server so don't store it in an entity data accessor

    // BOSS FIGHT
    private final ServerBossEvent server_boss_event = new ServerBossEvent(
            Component.literal("Tragon"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_12
    );
    private int boss_phase = 1;

    // DATA ACCESSORS
    private static final EntityDataAccessor<Boolean> FIRE_HEAD_IS_ALIVE = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIGHTNING_HEAD_IS_ALIVE = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ICE_HEAD_IS_ALIVE = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> FIRE_HEAD_MOUTH_POSITION = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> LIGHTNING_HEAD_MOUTH_POSITION = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> ICE_HEAD_MOUTH_POSITION = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> HEAD_PITCH = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.FLOAT); // this is for adjusting the pitch of the Tragon's heads in the model class
    private static final EntityDataAccessor<Integer> ATTACK_ACTION = SynchedEntityData.defineId(TragonEntity.class, EntityDataSerializers.INT); // actions need to be synched between client and server for animations

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

            // range
            ONE_HEAD_ATTACK, // only used when 1 head remains
            TWO_HEAD_ATTACK,
            THREE_HEAD_ATTACK // in phase 2 only
        }
    }

    // ANIMATIONS
    private final AnimatableInstanceCache geo_cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().then("animation.tragon.walk", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().then("animation.tragon.swim", Animation.LoopType.PLAY_ONCE);
    protected static final RawAnimation HIDE_ANIM = RawAnimation.begin().then("animation.tragon.hide", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation EXPOSE_ANIM = RawAnimation.begin().then("animation.tragon.expose", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation FIRE_HEAD_MOUTH_OPEN_ANIM = RawAnimation.begin().then("animation.tragon.fire_head_mouth_open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation LIGHTNING_HEAD_MOUTH_OPEN_ANIM = RawAnimation.begin().then("animation.tragon.lightning_head_mouth_open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation ICE_HEAD_MOUTH_OPEN_ANIM = RawAnimation.begin().then("animation.tragon.ice_head_mouth_open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation FIRE_HEAD_MOUTH_CLOSE_ANIM = RawAnimation.begin().then("animation.tragon.fire_head_mouth_close", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation LIGHTNING_HEAD_MOUTH_CLOSE_ANIM = RawAnimation.begin().then("animation.tragon.lightning_head_mouth_close", Animation.LoopType.HOLD_ON_LAST_FRAME);
    protected static final RawAnimation ICE_HEAD_MOUTH_CLOSE_ANIM = RawAnimation.begin().then("animation.tragon.ice_head_mouth_close", Animation.LoopType.HOLD_ON_LAST_FRAME);



    public TragonEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        // create hitboxes around the necks of the heads
        this.hitboxes = new PartEntity[] {
                new HitboxEntity(this, this.fire_head_neck_id, 2, 2),
                new HitboxEntity(this, this.lightning_head_neck_id, 2, 2),
                new HitboxEntity(this, this.ice_head_neck_id, 2, 2)
        };

        this.setId(ENTITY_COUNTER.getAndAdd(this.hitboxes.length + 1) + 1);

        // create head data (these are not the actual heads)
        float head_health = this.getHealth() / 3;

        this.heads.put(this.fire_head_neck_id, new FireHead(this, head_health));
        this.heads.put(this.lightning_head_neck_id, new LightningHead(this, head_health));
        this.heads.put(this.ice_head_neck_id, new IceHead(this, head_health));

        // add custom controls
        this.lookControl = new TragonLookControl(this); // change the default look control
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10 * 3) // CHANGE LATER
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

        controllers.add(new AnimationController<>(this, "fire_head_mouth_movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("fire_head_mouth_open", FIRE_HEAD_MOUTH_OPEN_ANIM)
                .triggerableAnim("fire_head_mouth_close", FIRE_HEAD_MOUTH_CLOSE_ANIM)
        );

        controllers.add(new AnimationController<>(this, "lightning_head_mouth_movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("lightning_head_mouth_open", LIGHTNING_HEAD_MOUTH_OPEN_ANIM)
                .triggerableAnim("lightning_head_mouth_close", LIGHTNING_HEAD_MOUTH_CLOSE_ANIM)
        );

        controllers.add(new AnimationController<>(this, "ice_head_mouth_movement_trigger_anim_controller", state -> PlayState.STOP)
                .triggerableAnim("ice_head_mouth_open", ICE_HEAD_MOUTH_OPEN_ANIM)
                .triggerableAnim("ice_head_mouth_close", ICE_HEAD_MOUTH_CLOSE_ANIM)
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

        // update health of heads
        String[] neck_ids = {
                "fire_head_neck",
                "lightning_head_neck",
                "ice_head_neck"
        }; // these should match the ones in the constructor

        for (String id : neck_ids) {
            float head_health = pCompound.getFloat(id);
            boolean has_taken_damage = pCompound.getBoolean(id + "_is_damaged");

            if (has_taken_damage) {
                TragonHead head = this.heads.get(id);

                head.setHealth(head_health);
                head.setHasTakenDamage(true);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // save health of heads
        for (Map.Entry<String, TragonHead> head_data : this.heads.entrySet()) {
            String neck_id = head_data.getKey();
            TragonHead head = head_data.getValue();

            float remaining_health = head.getHealth();
            pCompound.putFloat(neck_id, remaining_health);
            pCompound.putBoolean(neck_id + "_is_damaged", head.hasTakenDamage());
        }

        super.addAdditionalSaveData(pCompound);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder pBuilder) {
        super.defineSynchedData(pBuilder);

        // give data accessors starting values
        pBuilder.define(FIRE_HEAD_IS_ALIVE, true);
        pBuilder.define(LIGHTNING_HEAD_IS_ALIVE, true);
        pBuilder.define(ICE_HEAD_IS_ALIVE, true);

        pBuilder.define(FIRE_HEAD_MOUTH_POSITION, new Vector3f(0,0,0));
        pBuilder.define(LIGHTNING_HEAD_MOUTH_POSITION, new Vector3f(0,0,0));
        pBuilder.define(ICE_HEAD_MOUTH_POSITION, new Vector3f(0,0,0));

        pBuilder.define(HEAD_PITCH, 0.0f);
        pBuilder.define(ATTACK_ACTION, 0); // none
    }

    public void setHeadAliveFlag(TragonHead head, boolean isAlive) {
        if (head instanceof FireHead) {
            this.entityData.set(FIRE_HEAD_IS_ALIVE, isAlive);
        }
        else if (head instanceof LightningHead) {
            this.entityData.set(LIGHTNING_HEAD_IS_ALIVE, isAlive);
        }
        else if (head instanceof IceHead) {
            this.entityData.set(ICE_HEAD_IS_ALIVE, isAlive);
        }
    }

    public boolean getHeadAliveFlag(Class<? extends TragonHead> head) {
        if (head.isAssignableFrom(FireHead.class)) {
            return this.entityData.get(FIRE_HEAD_IS_ALIVE);
        }
        else if (head.isAssignableFrom(LightningHead.class)) {
            return this.entityData.get(LIGHTNING_HEAD_IS_ALIVE);
        }
        else if (head.isAssignableFrom(IceHead.class)) {
            return this.entityData.get(ICE_HEAD_IS_ALIVE);
        }

        // assume a head is alive by default
        return true;
    }

    public ArrayList<TragonHead> getAliveHeads() {
        ArrayList<TragonHead> heads_alive = new ArrayList<>();

        if (this.heads != null) {
            for (TragonHead head : this.heads.values()) {
                if (this.getHeadAliveFlag(head.getClass())) {
                    heads_alive.add(head);
                }
            }
        }

        return heads_alive;
    }

    public float getHeadPitch() {
        return this.entityData.get(HEAD_PITCH);
    }

    public void updateBonePosition(String boneName, Vec3 bonePos) {
        // this is called in the 'setCustomAnimations' method of the model class because that's where the bone positions can be accessed

        Vector3f bone_pos = new Vector3f((float) bonePos.x, (float) bonePos.y, (float) bonePos.z);

        switch (boneName) {
            case "fh_skull_mouth_lower":
                this.entityData.set(FIRE_HEAD_MOUTH_POSITION, bone_pos);
                break;
            case "lh_skull_mouth_lower":
                this.entityData.set(LIGHTNING_HEAD_MOUTH_POSITION, bone_pos);
                break;
            case "ih_skull_mouth_lower":
                this.entityData.set(ICE_HEAD_MOUTH_POSITION, bone_pos);
                break;
            default:
        }
    }

    public Vec3 getMouthPosition(Class<? extends TragonHead> head) {
        if (head.isAssignableFrom(FireHead.class)) {
            Vector3f mouth_pos = this.entityData.get(FIRE_HEAD_MOUTH_POSITION);
            return new Vec3(mouth_pos.x, mouth_pos.y, mouth_pos.z);
        }
        else if (head.isAssignableFrom(LightningHead.class)) {
            Vector3f mouth_pos = this.entityData.get(LIGHTNING_HEAD_MOUTH_POSITION);
            return new Vec3(mouth_pos.x, mouth_pos.y, mouth_pos.z);
        }
        else if (head.isAssignableFrom(IceHead.class)) {
            Vector3f mouth_pos = this.entityData.get(ICE_HEAD_MOUTH_POSITION);
            return new Vec3(mouth_pos.x, mouth_pos.y, mouth_pos.z);
        }

        return null;
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
        // this is called in the 'setCustomAnimations' method of the model class because that's where the bone positions can be accessed

        Level level = this.level();

        for (PartEntity<?> hitbox : this.hitboxes) {
            if (Objects.equals(((HitboxEntity) hitbox).getHitboxName(), hitboxName)) {
                if (!level.isClientSide) {
                    hitbox.setPos(bonePos);
                }
                else {
                    hitbox.moveTo(bonePos);
                }
            }
        }
    }

    private void removeNeckHitboxOfDeadHeads() {
        for (PartEntity<?> hitbox : this.hitboxes) {
            String hitbox_name = ((HitboxEntity) hitbox).getHitboxName();
            boolean remove_current_hitbox = false;

            // determine which hitbox needs to be removed
            if (Objects.equals(hitbox_name, this.fire_head_neck_id) && !this.getHeadAliveFlag(FireHead.class)) {
                remove_current_hitbox = true;
            }
            else if (Objects.equals(hitbox_name, this.lightning_head_neck_id) && !this.getHeadAliveFlag(LightningHead.class)) {
                remove_current_hitbox = true;
            }
            else if (Objects.equals(hitbox_name, this.ice_head_neck_id) && !this.getHeadAliveFlag(IceHead.class)) {
                remove_current_hitbox = true;
            }

            // handle removal
            if (remove_current_hitbox) {
                // delete from parts list
                this.hitboxes = ArrayUtils.removeElement(this.hitboxes, hitbox);

                // delete from game
                hitbox.discard();
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
            case Action.Attack.ONE_HEAD_ATTACK:
                action_id = 1;
                this.attack_type = Action.AttackType.RANGE;
                break;
            case Action.Attack.TWO_HEAD_ATTACK:
                action_id = 2;
                this.attack_type = Action.AttackType.RANGE;
                break;
            case Action.Attack.THREE_HEAD_ATTACK:
                action_id = 3;
                this.attack_type = Action.AttackType.RANGE;
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
                attack_action = Action.Attack.ONE_HEAD_ATTACK;
                break;
            case 2:
                attack_action = Action.Attack.TWO_HEAD_ATTACK;
                break;
            case 3:
                attack_action = Action.Attack.THREE_HEAD_ATTACK;
                break;
            default:
        }

        return attack_action;
    }

    public Action.AttackType getAttackType() {
        return this.attack_type;
    }

    public boolean isInDeepLiquid() {
        if (this.isInWater() || this.isInLava()) {
            Level level = this.level();
            BlockPos block_pos = BlockPos.containing(this.position());

            int threshold = 6; // no. blocks for the liquid to be considered deep
            int depth = 0;

            for (int i=0; i < threshold; i++) {
                if (!level.getBlockState(block_pos).getFluidState().isEmpty()) {
                    depth++;
                }

                block_pos = block_pos.below();
            }

            return depth == threshold;
        }

        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCloseToTarget() {
        LivingEntity target = this.getTarget();

        return target != null && this.distanceTo(target) <= 12;
    }

    @SuppressWarnings("SimplifiableConditionalExpression")
    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypeTags.IS_LIGHTNING) || pSource.is(DamageTypeTags.IS_FREEZING) || pSource.is(DamageTypes.CACTUS)) {
            // immune to these damages
            return false;
        }

        // OBJECTIVE: Damage the main body and slightly decrease the health of the remaining heads as well (i.e. make them weaker)

        float health_before_damage = this.getHealth();

        boolean is_hurt = this.isInvulnerable() || this.isInvulnerableTo(pSource) ? false : super.hurt(pSource, pAmount);

        float health_after_damage = this.getHealth();

        float damage = health_before_damage - health_after_damage;

        // apply a small portion of the damage to remaining heads
        damage = damage / 3;

        for (TragonHead head : this.heads.values()) {
            head.hurt(damage);
        }

        return is_hurt;
    }

    @SuppressWarnings("SimplifiableConditionalExpression")
    public boolean hurt(String hitboxName, @NotNull DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypeTags.IS_LIGHTNING) || pSource.is(DamageTypeTags.IS_FREEZING)) {
            // immune to lightning & freezing
            return false;
        }

        // OBJECTIVE: Damage the main body and the head whose neck was attacked

        float health_before_damage = this.getHealth();

        boolean is_hurt = this.isInvulnerable() || this.isInvulnerableTo(pSource) ? false : super.hurt(pSource, pAmount);

        float health_after_damage = this.getHealth();

        float damage = health_before_damage - health_after_damage;

        // apply the same damage to the head whose hitbox was hurt
        TragonHead head = this.heads.get(hitboxName);
        head.hurt(damage);

        return is_hurt;
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

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        super.die(pDamageSource);

        // discard hitboxes
        for (PartEntity<?> hitbox : this.hitboxes) {
            hitbox.discard();
        }
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt the Tragon
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new TragonEntity.SelectTargetGoal(this));
        
        /*
        HOW ATTACKING WORKS:
        - There are two types: MELEE and RANGE (see Action.AttackType enums)
        - All attack goals have Minecraft's 'TARGET' flag set which means they will conflict with the target selector goals. The priority of 1 means they will be executed instead of a target selector goal
        - Only one attack goal can run at a time so it doesn't matter that they all share the same priority number. The priority's only purpose is to stop the target selector goals when an attack goal is run
        - To determine which attack goal is run, their 'canUse' methods check which Action enums are active. These enums are set/replaced in the aiStep method
        */
        this.goalSelector.addGoal(1, new OneHeadAttackGoal(this));
        this.goalSelector.addGoal(1, new TwoHeadAttackGoal(this));
        this.goalSelector.addGoal(1, new ThreeHeadAttackGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        this.removeNeckHitboxOfDeadHeads();

        // update boss health bar
        float boss_health_remaining = this.getHealth() / this.getMaxHealth(); // in percentage
        this.server_boss_event.setProgress(boss_health_remaining);

        // update boss phase
        if (this.boss_phase == 1 && boss_health_remaining <= 0.5) {
            this.boss_phase = 2;

            ArrayList<TragonHead> alive_heads = this.getAliveHeads();

            for (TragonHead head : alive_heads) {
                head.activatePhase2();
            }
        }

        // handle general behaviour in water
        boolean in_deep_liquid = this.isInDeepLiquid();
        double allowed_depth_in_liquids = 3;

        if (in_deep_liquid) {
            this.setNoGravity(true); // don't sink in liquids

            this.triggerAnim("movement_trigger_anim_controller", "swim");
        }

        // handle movement & attack decisions
        LivingEntity target = this.getTarget();

        if (target != null) {
            boolean is_melee_attacking = this.getAttackAction() != Action.Attack.NONE && this.getAttackType() == Action.AttackType.MELEE;

            if (!target.isFallFlying()) {
                if (!is_melee_attacking) {
                    // follow target but keep a distance
                    this.getLookControl().setLookAt(target);

                    float distance_from_target = this.distanceTo(target);
                    int distance_to_keep_from_target = this.boss_phase == 2 ? 20 : 35; // stick close in phase 2 and stay far in phase 1

                    if (distance_from_target > distance_to_keep_from_target) {
                        Vec3 vector_to_target = target.position().subtract(this.position());

                        if (in_deep_liquid) {
                            // OBJECTIVE: Swim towards target when in deep liquids. Keep the lower body below the liquid

                            this.setDeltaMovement(vector_to_target.subtract(0, allowed_depth_in_liquids, 0).normalize().scale(0.7));

                            if (this.horizontalCollision) {
                                this.getJumpControl().jump(); // swim up
                            }
                        }
                        else {
                            // OBJECTIVE: Walk towards target when on land

                            this.setDeltaMovement(vector_to_target.normalize().scale(1));
                            this.setNoGravity(false);

                            if (this.horizontalCollision) {
                                this.jumpFromGround();
                            }

                            this.triggerAnim("movement_trigger_anim_controller", "walk");
                        }
                    }

                    // decide whether to do a melee or range attack
                    boolean is_attacking = this.getAttackAction() != Action.Attack.NONE;

                    if (!is_attacking) {
                        boolean should_attack = new Random().nextInt(1, 3) == 1; // 1 in 2 chances to attack

                        should_attack = true; // temp

                        if (should_attack) {
                            if (distance_from_target <= 8) {
                                // OBJECTIVE: Target got close. Stop following them and do a melee attack
                                this.setAttackAction(Action.Attack.NONE); // temp
                            }
                            else {
                                this.setAttackAction(Action.Attack.ONE_HEAD_ATTACK); // temp

                                // OBJECTIVE: Target is too far for a melee attack. Continue following them, while keeping a distance, and do a range attack
                                // int num_of_heads_alive = this.getAliveHeads().size();
                                //
                                // if (num_of_heads_alive == 3) {
                                //     boolean do_3_head_attack = new Random().nextInt(1, 3) == 1; // 50/50
                                //
                                //     if (do_3_head_attack) {
                                //         this.setAttackAction(Action.Attack.THREE_HEAD_ATTACK);
                                //     }
                                //     else {
                                //         this.setAttackAction(Action.Attack.TWO_HEAD_ATTACK);
                                //     }
                                // }
                                // else if (num_of_heads_alive == 2) {
                                //     boolean do_2_head_attack = new Random().nextInt(1, 3) == 1; // 50/50
                                //
                                //     if (do_2_head_attack) {
                                //         this.setAttackAction(Action.Attack.TWO_HEAD_ATTACK);
                                //     }
                                //     else {
                                //         this.setAttackAction(Action.Attack.ONE_HEAD_ATTACK);
                                //     }
                                // }
                                // else if (num_of_heads_alive == 1) {
                                //     this.setAttackAction(Action.Attack.ONE_HEAD_ATTACK);
                                // }
                            }
                        }
                    }
                }
            }
        }
    }



    // CONTROLS
    private static class TragonLookControl extends LookControl {
        public TragonLookControl(Mob pMob) {
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

            // set pitch of heads to face target
            float new_head_pitch = this.mob.getXRot();

            if (new_head_pitch != 0) {
                this.mob.getEntityData().set(HEAD_PITCH, (float) -Math.toRadians(new_head_pitch)); // GeckoLib uses radians. Rotation is done in the 'setCustomAnimations' method of the model class
            }
        }
    }



    // CUSTOM GOALS
    private static class SelectTargetGoal extends NearestAttackableTargetGoal<Player> {
        public SelectTargetGoal(Mob pMob) {
            // this is a custom constructor made to reduce the amount of parameters. It doesn't override any constructor from the parent

            this(pMob, Player.class, 10, true, false, null);
        }

        public SelectTargetGoal(Mob pMob, Class<Player> pTargetType, int pRandomInterval, boolean pMustSee, boolean pMustReach, @Nullable Predicate<LivingEntity> pTargetPredicate) {
            // this is the main constructor where the target conditions are set (see NearestAttackableTargetGoal). It was overridden to increase how far the Tragon can spot targets

            super(pMob, pTargetType, pRandomInterval, pMustSee, pMustReach, pTargetPredicate);

            final double MAX_TARGET_DISTANCE = 60d; // blocks
            this.targetConditions = TargetingConditions
                    .forCombat()
                    .ignoreLineOfSight() // allow Tragon to continue following a target even if they're obstructed by the environment, e.g. under trees
                    .range(MAX_TARGET_DISTANCE)
                    .selector(pTargetPredicate);
        }
    }

    private static class OneHeadAttackGoal extends Goal {
        private final TragonEntity tragon;
        private TragonHead attacking_head = null;
        private boolean attack_is_finished = false;

        public OneHeadAttackGoal(TragonEntity tragon) {
            this.tragon = tragon;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        private void resetAttack() {
            this.attacking_head = null;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            if (this.attacking_head == null) {
                ArrayList<TragonHead> alive_heads = this.tragon.getAliveHeads();

                // choose the Tragon head that will attack
                // this.attacking_head = alive_heads.get(new Random().nextInt(0, alive_heads.size()));

                // temp
                for (TragonHead head : alive_heads) {
                    if (head instanceof IceHead) {
                        this.attacking_head = head;
                        break;
                    }
                }

                this.attacking_head.chooseAttack();
            }

            super.start();
        }

        @Override
        public void stop() {
            if (this.attack_is_finished) {
                this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

                this.tragon.setAttackAction(Action.Attack.NONE); // allow Tragon to choose another attack
            }
        }

        @Override
        public void tick() {
            if (this.attacking_head != null) {
                // OBJECTIVE: Continuously run the attack tick of the head until its attack is finished

                if (!this.attacking_head.isFinishedAttacking()) {
                    this.attacking_head.attackTick();
                }
                else {
                    this.attack_is_finished = true;
                }
            }
            else {
                // cancel attack
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.tragon.getAttackType() == Action.AttackType.RANGE && this.tragon.getAttackAction() == Action.Attack.ONE_HEAD_ATTACK;
        }
    }

    private static class TwoHeadAttackGoal extends Goal {
        private final TragonEntity tragon;
        private ArrayList<TragonHead> attacking_heads = null;
        private boolean attack_is_finished = false;

        public TwoHeadAttackGoal(TragonEntity tragon) {
            this.tragon = tragon;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        private void resetAttack() {
            this.attacking_heads = null;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            if (this.attacking_heads == null) {
                this.attacking_heads = this.tragon.getAliveHeads();

                // choose the Tragon heads that will attack by randomly selecting one to remove
                if (this.attacking_heads.size() > 2) {
                    int index = new Random().nextInt(0, this.attacking_heads.size());
                    this.attacking_heads.remove(index);
                }

                for (TragonHead head : this.attacking_heads) {
                    // randomly choose which attack each head will do
                    head.chooseAttack();
                }
            }

            super.start();
        }

        @Override
        public void stop() {
            if (this.attack_is_finished) {
                this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

                this.tragon.setAttackAction(Action.Attack.NONE); // allow Tragon to choose another attack
            }

            super.stop();
        }

        @Override
        public void tick() {
            // OBJECTIVE: Continuously run the attack ticks of each head until their attack is finished

            if (this.attacking_heads != null && this.attacking_heads.size() == 2) {
                TragonHead head1 = this.attacking_heads.getFirst();
                TragonHead head2 = this.attacking_heads.get(1);

                boolean head1_is_finished_attacking = head1.isFinishedAttacking();
                boolean head2_is_finished_attacking = head2.isFinishedAttacking();

                if (!head1_is_finished_attacking) {
                    head1.attackTick();
                }

                if (!head2_is_finished_attacking) {
                    head2.attackTick();
                }

                if (head1_is_finished_attacking && head2_is_finished_attacking) {
                    this.attack_is_finished = true;
                }
            }
            else {
                // cancel attack
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.tragon.getAttackType() == Action.AttackType.RANGE && this.tragon.getAttackAction() == Action.Attack.TWO_HEAD_ATTACK;
        }
    }

    private static class ThreeHeadAttackGoal extends Goal {
        private final TragonEntity tragon;
        private ArrayList<TragonHead> attacking_heads = null;
        private boolean attack_is_finished = false;

        public ThreeHeadAttackGoal(TragonEntity tragon) {
            this.tragon = tragon;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        private void resetAttack() {
            this.attacking_heads = null;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            if (this.attacking_heads == null) {
                this.attacking_heads = this.tragon.getAliveHeads();

                for (TragonHead head : this.attacking_heads) {
                    // randomly choose which attack each head will do
                    head.chooseAttack();
                }
            }

            super.start();
        }

        @Override
        public void stop() {
            if (this.attack_is_finished) {
                this.resetAttack(); // this is needed because the goal instance is re-used which means all the data needs to be reset to allow it to pass the 'canUse' test next time

                this.tragon.setAttackAction(Action.Attack.NONE); // allow Tragon to choose another attack
            }

            super.stop();
        }

        @Override
        public void tick() {
            // OBJECTIVE: Continuously run the attack ticks of each head until their attack is finished

            if (this.attacking_heads != null && this.attacking_heads.size() == 3) {
                TragonHead head1 = this.attacking_heads.getFirst();
                TragonHead head2 = this.attacking_heads.get(1);
                TragonHead head3 = this.attacking_heads.get(2);

                boolean head1_is_finished_attacking = head1.isFinishedAttacking();
                boolean head2_is_finished_attacking = head2.isFinishedAttacking();
                boolean head3_is_finished_attacking = head3.isFinishedAttacking();

                if (!head1_is_finished_attacking) {
                    head1.attackTick();
                }

                if (!head2_is_finished_attacking) {
                    head2.attackTick();
                }

                if (!head3_is_finished_attacking) {
                    head3.attackTick();
                }

                if (head1_is_finished_attacking && head2_is_finished_attacking && head3_is_finished_attacking) {
                    this.attack_is_finished = true;
                }
            }
            else {
                // cancel attack
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && this.tragon.getAttackType() == Action.AttackType.RANGE && this.tragon.getAttackAction() == Action.Attack.THREE_HEAD_ATTACK;
        }
    }
}

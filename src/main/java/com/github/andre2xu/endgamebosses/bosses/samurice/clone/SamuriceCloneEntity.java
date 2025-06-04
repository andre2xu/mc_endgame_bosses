package com.github.andre2xu.endgamebosses.bosses.samurice.clone;

import com.github.andre2xu.endgamebosses.bosses.samurice.SamuriceEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class SamuriceCloneEntity extends SamuriceEntity {
    public SamuriceCloneEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .build();
    }



    // DATA
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {}

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {}



    // BOSS HEALTHBAR (disabled)
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer pServerPlayer) {}

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer pServerPlayer) {}



    // AI
    @Override
    protected int getBaseExperienceReward() {
        int xp = 0;

        if (this.level() instanceof ServerLevel server_level) {
            Difficulty difficulty = server_level.getDifficulty();

            xp = switch (difficulty) {
                case Difficulty.EASY -> 100;
                case Difficulty.NORMAL -> 200;
                case Difficulty.HARD -> 300;
                default -> xp;
            };
        }

        return xp;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected void registerGoals() {
        // target the player that hurt the clone
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Player.class));

        // find and select a target
        this.targetSelector.addGoal(3, new SelectTargetGoal(this));

        // handle blocking (see SamuriceEntity for the goal definition)
        this.goalSelector.addGoal(1, new BlockGoal(this));

        // handle attacks (see SamuriceEntity for the goal definitions)
        this.goalSelector.addGoal(1, new DashAttackGoal(this));
        this.goalSelector.addGoal(1, new CutsAttackGoal(this));
    }

    @Override
    public void aiStep() {
        // NOTE: all the logic is the same as the parent's aiStep except there are no boss-related code

        super.runBaseAiStep();

        // heal in water
        if (this.isInWater() && this.isAlive() && this.tickCount % 20 == 0) {
            this.setHealth(this.getHealth() + 2); // 1 heart per second
        }

        // handle movement & attack decisions
        LivingEntity target = this.getTarget();

        if (target != null) {
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
                                this.setAttackAction(Action.Attack.CUTS);
                            }
                        }
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
}

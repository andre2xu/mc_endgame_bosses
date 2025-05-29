package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.ProjectilesRegistry;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.icicle.TragonIcicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Random;

public class IceHead extends TragonHead {
    private final TragonEntity parent;

    public IceHead(TragonEntity parent, float maxHealth, String neckHitboxId) {
        super(parent, maxHealth, neckHitboxId);

        this.parent = parent;

        this.addAttack(new FrostBreath(parent));
    }

    @Override
    public void activatePhase2() {
        super.activatePhase2();

        this.addAttack(new Icicles(this.parent));
    }



    // ATTACKING
    private static class FrostBreath implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private ArrayList<Vec3> breath_path = new ArrayList<>();
        private int breath_duration = 0;
        private boolean breath_touches_target = false;
        private final float attack_damage = 1f; // CHANGE LATER
        private int attack_delay = 0;
        private boolean attack_is_finished = false;

        public FrostBreath(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private void calculateBreathPath(Vec3 startPos, Vec3 endPos) {
            this.breath_path = new ArrayList<>(); // reset path

            Vec3 current_point = startPos;
            Vec3 direction = endPos.subtract(startPos).normalize(); // 1 block step towards target

            this.breath_path.add(current_point); // add starting point to path

            int reach = 25; // blocks

            for (int i=0; i < reach; i++) {
                current_point = current_point.add(direction); // get next point

                this.breath_path.add(current_point);
            }
        }

        private void freezeSurface(Vec3 startingPoint) {
            if (this.tragon.level() instanceof ServerLevel server_level) {
                int num_of_blocks_to_check = 3;

                BlockPos block_pos = BlockPos.containing(startingPoint);

                for (int i=0; i < num_of_blocks_to_check; i++) {
                    BlockState state_of_current_block = server_level.getBlockState(block_pos);

                    if (state_of_current_block.isAir()) {
                        // skip to the block below
                        block_pos = block_pos.below();
                    }
                    else {
                        BlockPos block_above = block_pos.above();
                        boolean block_above_is_air = server_level.getBlockState(block_above).isAir();
                        boolean block_has_a_solid_top_face = state_of_current_block.isFaceSturdy(server_level, block_pos, Direction.UP);

                        boolean is_surface_block = block_above_is_air && block_has_a_solid_top_face;
                        boolean is_water_surface = block_above_is_air && state_of_current_block.getFluidState().is(Fluids.WATER);

                        if (is_surface_block || state_of_current_block.is(BlockTags.LEAVES)) {
                            // add a snow layer above the surface block or leaf block
                            server_level.setBlockAndUpdate(block_above, Blocks.SNOW.defaultBlockState());
                            break;
                        }
                        else if (is_water_surface) {
                            // convert water surface to ice
                            server_level.setBlockAndUpdate(block_pos, Blocks.ICE.defaultBlockState());
                            break;
                        }
                        else {
                            // move to the block below
                            block_pos = block_pos.below();
                        }
                    }
                }
            }
        }

        private void decrementBreathDuration() {
            if (this.breath_duration > 0) {
                this.breath_duration--;
            }
        }

        private void resetBreathDuration() {
            this.breath_duration = 20 * 3; // 3 seconds
        }

        private void decrementAttackDelay() {
            if (this.attack_delay > 0) {
                this.attack_delay--;
            }
        }

        private void resetAttackDelay() {
            this.attack_delay = 20 * 2; // 2 seconds
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(IceHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        @Override
        public void resetAttack() {
            this.target = null;
            this.breath_path = new ArrayList<>(); // clear memory
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.tragon.getTarget' which can sometimes return null
            this.target = this.tragon.getTarget();

            // add a delay before the attack
            this.resetAttackDelay();

            // reset breath duration
            this.resetBreathDuration();
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack() && this.tragon.level() instanceof ServerLevel server_level) {
                Vec3 mouth_pos = this.tragon.getMouthPosition(IceHead.class);
                Vec3 target_pos = this.target.position();

                // correct the mouth's x and z position
                mouth_pos = mouth_pos.add(target_pos.subtract(mouth_pos.x, 0, mouth_pos.z).normalize().multiply(5, 0, 5));

                // correct the y position
                double y_correction = 1;
                float head_pitch = this.tragon.getHeadPitch();

                if (head_pitch < 0.1) {
                    if (head_pitch <= -0.21) {
                        y_correction = 2;
                    }
                    else if (head_pitch <= -0.31) {
                        y_correction = 3;
                    }

                    mouth_pos = mouth_pos.subtract(0, y_correction, 0);
                }

                int particle_count = 10;
                double particle_speed = 0.02;

                // generate frost particles in mouth
                server_level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        mouth_pos.x, mouth_pos.y, mouth_pos.z,
                        particle_count,
                        0, 0, 0,
                        particle_speed
                );

                if (this.attack_delay > 0) {
                    this.decrementAttackDelay();
                }
                else {
                    // update breath path
                    this.calculateBreathPath(mouth_pos, target_pos);

                    if (this.breath_duration > 0) {
                        // breathe frost
                        for (Vec3 point : this.breath_path) {
                            if (this.breath_duration % 10 == 0) {
                                this.tragon.playSound(SoundEvents.PLAYER_HURT_FREEZE, 2f, 0.5f);
                            }

                            server_level.sendParticles(
                                    ParticleTypes.SNOWFLAKE,
                                    point.x, point.y, point.z,
                                    particle_count,
                                    0, 0, 0,
                                    particle_speed
                            );

                            // freeze ground, leaves, or water close to frost breath
                            this.freezeSurface(point);

                            // check if target is close to a point
                            if (!this.breath_touches_target && Math.sqrt(this.target.distanceToSqr(point)) <= 1) {
                                this.breath_touches_target = true;
                            }
                        }

                        // inflict damage & freezing to target
                        if (this.breath_touches_target) {
                            this.target.hurt(this.tragon.damageSources().dragonBreath(), this.attack_damage);

                            int freezing_effect_duration = 20 * 2; // 2 seconds

                            // make the target slower
                            MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezing_effect_duration);

                            this.target.addEffect(slowness);

                            // give them Minecraft's 'freeze' effect
                            this.target.setTicksFrozen(freezing_effect_duration);
                        }

                        // reset flag
                        this.breath_touches_target = false;

                        this.decrementBreathDuration();
                    }
                    else {
                        // stop attack
                        this.attack_is_finished = true;
                    }
                }
            }
            else {
                // cancel attack if ice head is dead, target doesn't exist, target is dead, target is too close, or target is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && !this.tragon.isCloseToTarget();
        }
    }

    private static class Icicles implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private int num_of_icicles_to_spawn = 0;
        private int attack_delay = 0;
        private boolean attack_is_finished = false;

        public Icicles(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private void decrementAttackDelay() {
            if (this.attack_delay > 0) {
                this.attack_delay--;
            }
        }

        private void resetAttackDelay() {
            this.attack_delay = 20 * new Random().nextInt(1, 4); // 1 to 3 seconds
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(IceHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        @Override
        public void resetAttack() {
            this.target = null;
            this.num_of_icicles_to_spawn = 0;
            this.attack_delay = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.tragon.getTarget' which can sometimes return null
            this.target = this.tragon.getTarget();

            // pick a random amount of icicles to spawn for attack
            this.num_of_icicles_to_spawn = new Random().nextInt(2, 6); // 2 to 5

            // set a delay for the first attack
            this.resetAttackDelay();
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack() && this.tragon.level() instanceof ServerLevel server_level) {
                Vec3 mouth_pos = this.tragon.getMouthPosition(IceHead.class);
                Vec3 target_pos = this.target.position();

                // correct the mouth's x and z position
                mouth_pos = mouth_pos.add(target_pos.subtract(mouth_pos.x, 0, mouth_pos.z).normalize().multiply(5, 0, 5));

                // correct the y position
                double y_correction = 1;
                float head_pitch = this.tragon.getHeadPitch();

                if (head_pitch < 0.1) {
                    if (head_pitch <= -0.21) {
                        y_correction = 2;
                    }
                    else if (head_pitch <= -0.31) {
                        y_correction = 3;
                    }

                    mouth_pos = mouth_pos.subtract(0, y_correction, 0);
                }

                int particle_count = 10;
                double particle_speed = 0.02;

                // generate frost particles in mouth
                server_level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        mouth_pos.x, mouth_pos.y, mouth_pos.z,
                        particle_count,
                        0, 0, 0,
                        particle_speed
                );

                if (this.attack_delay > 0) {
                    // play a sound
                    if (this.num_of_icicles_to_spawn > 0 && this.attack_delay > 3 && this.attack_delay <= 6) {
                        this.tragon.playSound(SoundEvents.PLAYER_HURT_FREEZE, 10f, 0.5f);
                    }

                    this.decrementAttackDelay();
                }
                else {
                    if (this.num_of_icicles_to_spawn > 0) {
                        TragonIcicleEntity icicle = ProjectilesRegistry.TRAGON_ICICLE.get().create(server_level);

                        if (icicle != null) {
                            icicle.setPos(this.target.position().add(0, 15, 0)); // appear above the target

                            server_level.addFreshEntity(icicle);
                        }

                        this.num_of_icicles_to_spawn--;

                        // set a delay for the next attack
                        this.resetAttackDelay();
                    }
                    else {
                        // stop attack
                        this.attack_is_finished = true;
                    }
                }
            }
            else {
                // cancel attack if ice head is dead, target doesn't exist, target is dead, target is too close, or target is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && !this.tragon.isCloseToTarget();
        }
    }
}

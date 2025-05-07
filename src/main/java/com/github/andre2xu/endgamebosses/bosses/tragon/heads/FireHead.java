package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Random;

public class FireHead extends TragonHead {
    private final TragonEntity parent;

    public FireHead(TragonEntity parent, float maxHealth, String neckHitboxId) {
        super(parent, maxHealth, neckHitboxId);

        this.parent = parent;

        this.addAttack(new Fireballs(parent));
    }

    @Override
    public void activatePhase2() {
        super.activatePhase2();

        this.addAttack(new FireBreath(this.parent));
    }



    // ATTACKING
    private static class Fireballs implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private int shoot_cooldown = 0; // no cooldown for first fireball
        private int num_of_fireballs_to_shoot = 0;
        private boolean attack_is_finished = false;

        public Fireballs(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private void decrementShootCooldown() {
            if (this.shoot_cooldown > 0) {
                this.shoot_cooldown--;
            }
        }

        private void resetShootCooldown() {
            this.shoot_cooldown = 20; // 1 second
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(FireHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        @Override
        public void resetAttack() {
            this.target = null;
            this.shoot_cooldown = 0;
            this.num_of_fireballs_to_shoot = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.tragon.getTarget' which can sometimes return null
            this.target = this.tragon.getTarget();

            // decide how many fireballs will be shot
            this.num_of_fireballs_to_shoot = new Random().nextInt(3, 6); // 3 to 5 fireballs will be shot
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                if (this.num_of_fireballs_to_shoot > 0) {
                    if (this.shoot_cooldown > 0) {
                        this.decrementShootCooldown();
                    }
                    else {
                        // OBJECTIVE: Spawn fireball at the fire head's mouth and shoot it at target

                        if (this.tragon.level() instanceof ServerLevel server_level) {
                            Vec3 mouth_pos = this.tragon.getMouthPosition(FireHead.class);
                            Vec3 target_pos = this.target.position();

                            double fireball_spawn_y_offset = 3;

                            Vec3 direction = target_pos.subtract(mouth_pos).add(0, fireball_spawn_y_offset, 0); // from mouth to target
                            Vec3 fireball_spawn_point = mouth_pos.subtract(0, fireball_spawn_y_offset, 0).add(direction.normalize().scale(1)); // adjust the spawn position so that it aligns better with the mouth

                            server_level.addFreshEntity(new CustomFireball(
                                    fireball_spawn_point,
                                    this.tragon,
                                    direction,
                                    server_level
                            ));
                        }

                        // decrease the fireballs counter and start attack cooldown
                        this.num_of_fireballs_to_shoot--;

                        this.resetShootCooldown();
                    }
                }
                else {
                    // stop attack
                    this.attack_is_finished = true;
                }
            }
            else {
                // cancel attack if lightning head is dead, target doesn't exist, target is dead, target is too close, or target is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && !this.tragon.isCloseToTarget();
        }



        private static class CustomFireball extends AbstractHurtingProjectile {
            private final LivingEntity owner;

            public CustomFireball(Vec3 origin, LivingEntity pOwner, Vec3 pMovement, Level pLevel) {
                super(EntityType.FIREBALL, origin.x, origin.y, origin.z, pMovement, pLevel);
                this.setOwner(pOwner);
                this.setRot(pOwner.getYRot(), pOwner.getXRot());

                this.owner = pOwner;
            }

            @Override
            protected void onHit(@NotNull HitResult pResult) {
                super.onHit(pResult);

                if (this.level() instanceof ServerLevel server_level) {
                    server_level.explode(
                            this,
                            this.damageSources().mobProjectile(this, this.owner),
                            new CustomExplosionDamageCalculator(), // don't damage the Tragon
                            this.position(), // cause explosion at landing spot
                            3, // radius in blocks
                            server_level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING), // spawn fire
                            Level.ExplosionInteraction.MOB
                    );

                    // delete fireball in game
                    this.discard();
                }
            }

            private static class CustomExplosionDamageCalculator extends ExplosionDamageCalculator {
                @Override
                public boolean shouldDamageEntity(@NotNull Explosion pExplosion, @NotNull Entity pEntity) {
                    if (pEntity instanceof TragonEntity || pEntity instanceof HitboxEntity) {
                        return false;
                    }

                    return super.shouldDamageEntity(pExplosion, pEntity);
                }
            }
        }
    }

    private static class FireBreath implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private ArrayList<Vec3> breath_path = new ArrayList<>();
        private int breath_duration = 0;
        private boolean breath_touches_target = false;
        private final float attack_damage = 1f; // CHANGE LATER
        private int attack_delay = 0;
        private boolean attack_is_finished = false;

        public FireBreath(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private void calculateBreathPath(Vec3 startPos, Vec3 endPos) {
            this.breath_path = new ArrayList<>(); // reset path

            Vec3 current_point = startPos;
            Vec3 direction = endPos.subtract(startPos).normalize(); // 1 block step towards target

            this.breath_path.add(current_point); // add starting point to path

            int reach = 20; // blocks

            for (int i=0; i < reach; i++) {
                current_point = current_point.add(direction); // get next point

                this.breath_path.add(current_point);
            }
        }

        private void igniteSurface(Vec3 startingPoint) {
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

                        if (is_surface_block || state_of_current_block.is(BlockTags.LEAVES)) {
                            // add fire above the surface block or leaf block
                            server_level.setBlockAndUpdate(block_above, Blocks.FIRE.defaultBlockState());
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
            return this.tragon != null && this.tragon.getHeadAliveFlag(FireHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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
                // spawn flame particles in the fire head's mouth
                Vec3 mouth_pos = this.tragon.getMouthPosition(FireHead.class);
                Vec3 target_pos = this.target.position();

                double flame_spawn_y_offset = 3;

                Vec3 vector_to_target = target_pos.subtract(mouth_pos).add(0, flame_spawn_y_offset, 0); // from mouth to target
                Vec3 flame_spawn_point = mouth_pos.subtract(0, flame_spawn_y_offset, 0).add(vector_to_target.normalize().scale(1)); // adjust the spawn position so that it aligns better with the mouth

                int particle_count = 10;
                double particle_speed = 0.02;

                // generate flame particles
                server_level.sendParticles(
                        ParticleTypes.FLAME,
                        flame_spawn_point.x, flame_spawn_point.y, flame_spawn_point.z,
                        particle_count,
                        0, 0, 0,
                        particle_speed
                );

                if (this.attack_delay > 0) {
                    this.decrementAttackDelay();
                }
                else {
                    // update breath path
                    this.calculateBreathPath(flame_spawn_point, target_pos);

                    if (this.breath_duration > 0) {
                        // breathe flames
                        for (Vec3 point : this.breath_path) {
                            server_level.sendParticles(
                                    ParticleTypes.FLAME,
                                    point.x, point.y, point.z,
                                    particle_count,
                                    0, 0, 0,
                                    particle_speed
                            );

                            // ignite ground and leaves
                            this.igniteSurface(point);

                            // check if target is close to a point
                            if (!this.breath_touches_target && Math.sqrt(this.target.distanceToSqr(point)) <= 1) {
                                this.breath_touches_target = true;
                            }
                        }

                        // inflict damage to target & set them on fire
                        if (this.breath_touches_target) {
                            this.target.hurt(this.tragon.damageSources().dragonBreath(), this.attack_damage);

                            int ignite_effect_duration = 20 * 2; // 2 seconds

                            this.target.setRemainingFireTicks(ignite_effect_duration);
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
                // cancel attack if fire head is dead, target doesn't exist, target is dead, target is too close, or target is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished && !this.tragon.isCloseToTarget();
        }
    }
}

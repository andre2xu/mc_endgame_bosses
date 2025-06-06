package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Random;

public class LightningHead extends TragonHead {
    private final TragonEntity parent;

    public LightningHead(TragonEntity parent, float maxHealth, String neckHitboxId) {
        super(parent, maxHealth, neckHitboxId);

        this.parent = parent;

        this.addAttack(new LightningStrikes(parent));
    }

    @Override
    public void activatePhase2() {
        super.activatePhase2();

        this.addAttack(new LaserBeam(this.parent));
    }



    // ATTACKING
    private static class LightningStrikes implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private BlockPos target_pos = null;
        private int num_of_lightning_strikes_to_summon = 0;
        private int attack_delay = 0;
        private boolean attack_is_finished = false;

        public LightningStrikes(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private void resetAttackDelay() {
            this.attack_delay = 20 * new Random().nextInt(1, 3); // 1 to 2 second delay
        }

        private void decrementAttackDelay() {
            if (this.attack_delay > 0) {
                this.attack_delay--;
            }
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(LightningHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        @Override
        public void resetAttack() {
            this.target = null;
            this.target_pos = null;
            this.num_of_lightning_strikes_to_summon = 0;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.tragon.getTarget' which can sometimes return null
            this.target = this.tragon.getTarget();

            // pick a random number of lightning strikes to summon
            this.num_of_lightning_strikes_to_summon = new Random().nextInt(4, 6); // 4 to 5

            // set the delay for the first attack
            this.resetAttackDelay();
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                Vec3 mouth_pos = this.tragon.getMouthPosition(LightningHead.class);
                Vec3 vector_to_target = this.target.position().subtract(mouth_pos).normalize().scale(1.5);

                mouth_pos = mouth_pos.add(vector_to_target.x, 0, vector_to_target.z); // spawn position is in front of mouth

                if (this.tragon.level() instanceof ServerLevel server_level) {
                    // make mouth of lightning head glow
                    server_level.sendParticles(
                            ParticleTypes.END_ROD,
                            mouth_pos.x, mouth_pos.y - 3, mouth_pos.z,
                            5, // particle count
                            0, 0, 0,
                            0.04 // speed
                    );

                    // summon lightning strikes
                    if (this.num_of_lightning_strikes_to_summon > 0) {
                        // OBJECTIVE: Count down before summoning a lightning strike at the target's position

                        if (this.attack_delay > 0) {
                            this.decrementAttackDelay();

                            if (this.attack_delay == 5) {
                                // save target position when the delay has 0.25 seconds left. This is done so the lightning can be dodged but barely
                                this.target_pos = this.target.blockPosition();
                            }

                            // play a sound periodically
                            if (this.attack_delay % 5 == 0) {
                                this.tragon.playSound(SoundEvents.BEACON_POWER_SELECT, 2f, 1f);
                            }
                        }
                        else {
                            if (this.target_pos != null) {
                                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(
                                        server_level,
                                        null,
                                        this.target_pos,
                                        MobSpawnType.MOB_SUMMONED,
                                        false,
                                        false
                                );

                                if (lightning != null) {
                                    // determine attack damage (relative to full un-enchanted diamond armor)
                                    Difficulty difficulty = server_level.getDifficulty();

                                    float attack_damage = switch (difficulty) {
                                        case Difficulty.EASY -> 14; // 2 hearts
                                        case Difficulty.NORMAL -> 20; // 4 hearts
                                        case Difficulty.HARD -> 27; // 6 hearts
                                        default -> 0;
                                    };

                                    lightning.setDamage(attack_damage);

                                    // spawn lightning
                                    server_level.addFreshEntity(lightning);
                                }

                                // set a delay for the next strike
                                this.resetAttackDelay();

                                // decrease the amount of strikes left
                                this.num_of_lightning_strikes_to_summon--;
                            }
                        }
                    }
                    else {
                        // stop attack
                        this.attack_is_finished = true;
                    }
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
    }

    private static class LaserBeam implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private Vec3 target_pos = null;
        private ArrayList<Vec3> beam_path = new ArrayList<>();
        private boolean beam_touches_target = false;
        private int attack_delay = 0;
        private boolean attack_is_finished = false;

        public LaserBeam(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private void hurtTarget() {
            // determine attack damage (relative to full un-enchanted diamond armor)
            Difficulty difficulty = this.tragon.level().getDifficulty();

            float attack_damage = switch (difficulty) {
                case Difficulty.EASY -> 6; // 3 hearts
                case Difficulty.NORMAL -> 10; // 5 hearts
                case Difficulty.HARD -> 14; // 7 hearts
                default -> 0;
            };

            // damage target
            this.target.hurt(this.tragon.damageSources().dragonBreath(), attack_damage);

            // blind target
            int blindness_effect_duration = 20 * 4; // 4 seconds

            MobEffectInstance blindness = new MobEffectInstance(MobEffects.BLINDNESS, blindness_effect_duration);
            this.target.addEffect(blindness);
        }

        private void calculateBeamPath(Vec3 startPos, Vec3 endPos) {
            this.beam_path = new ArrayList<>(); // reset path

            Vec3 current_point = startPos;
            Vec3 direction = endPos.subtract(startPos).normalize(); // 1 block step towards target

            this.beam_path.add(current_point); // add starting point to path

            int reach = 40; // blocks

            for (int i=0; i < reach; i++) {
                current_point = current_point.add(direction); // get next point

                this.beam_path.add(current_point);
            }
        }

        private void decrementAttackDelay() {
            if (this.attack_delay > 0) {
                this.attack_delay--;
            }
        }

        private void resetAttackDelay() {
            this.attack_delay = 20 * 3; // 3 seconds
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(LightningHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
        }

        @Override
        public void resetAttack() {
            this.target = null;
            this.target_pos = null;
            this.beam_path = new ArrayList<>(); // clear memory
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.tragon.getTarget' which can sometimes return null
            this.target = this.tragon.getTarget();

            // add a delay before the attack
            this.resetAttackDelay();
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack() && this.tragon.level() instanceof ServerLevel server_level) {
                // make mouth of lightning head glow
                Vec3 mouth_pos = this.tragon.getMouthPosition(LightningHead.class);
                Vec3 vector_to_target = this.target.position().subtract(mouth_pos).normalize().scale(1.5);

                mouth_pos = mouth_pos.add(vector_to_target.x, 0, vector_to_target.z);
                mouth_pos = mouth_pos.subtract(0, 3, 0);

                int particle_count = 5;
                double particle_speed = 0.04;

                server_level.sendParticles(
                        ParticleTypes.END_ROD,
                        mouth_pos.x, mouth_pos.y, mouth_pos.z,
                        particle_count,
                        0, 0, 0,
                        particle_speed
                );

                if (this.attack_delay > 0) {
                    this.decrementAttackDelay();

                    if (this.attack_delay == 1) {
                        // save target position when the delay has 0.05 seconds left. This is done so the beam can be dodged but barely
                        this.target_pos = this.target.position();
                    }

                    // play a charge-up sound
                    if (this.attack_delay > 20 && this.attack_delay % 5 == 0) {
                        this.tragon.playSound(SoundEvents.BEACON_ACTIVATE, 3f, 1f);
                    }
                }
                else {
                    // update beam path
                    this.calculateBeamPath(mouth_pos, this.target_pos);

                    // shoot beam
                    for (Vec3 point : this.beam_path) {
                        server_level.sendParticles(
                                ParticleTypes.END_ROD,
                                point.x, point.y, point.z,
                                particle_count * 2,
                                0, 0, 0,
                                0.01
                        );

                        // check if target is close to a point
                        if (!this.beam_touches_target && Math.sqrt(this.target.distanceToSqr(point)) <= 1) {
                            this.beam_touches_target = true;
                        }
                    }

                    // inflict damage to target & blindness
                    if (this.beam_touches_target) {
                        this.hurtTarget();
                    }

                    // reset flag
                    this.beam_touches_target = false;

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
    }
}

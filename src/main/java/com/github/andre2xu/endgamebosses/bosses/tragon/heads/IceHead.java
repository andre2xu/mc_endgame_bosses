package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class IceHead extends TragonHead {
    public IceHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new FrostBreath(parent));
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
            return this.tragon != null && this.tragon.getHeadAliveFlag(IceHead.class) && this.target != null && this.target.isAlive() && this.tragon.distanceTo(this.target) > 12 && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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
                // spawn frost particles in the ice head's mouth
                Vec3 mouth_pos = this.tragon.getMouthPosition(IceHead.class);
                Vec3 target_pos = this.target.position();

                mouth_pos = mouth_pos.add(target_pos.subtract(mouth_pos.x, 0, mouth_pos.z).normalize().scale(4)); // correct the mouth position

                server_level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        mouth_pos.x, mouth_pos.y + 2, mouth_pos.z,
                        10, // particle count
                        0, 0, 0,
                        0.02 // speed
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
                            server_level.sendParticles(
                                    ParticleTypes.SNOWFLAKE,
                                    point.x, point.y + 2, point.z,
                                    10, // particle count
                                    0, 0, 0,
                                    0.02 // speed
                            );

                            // check if target is close to a point
                            if (!this.breath_touches_target && Math.sqrt(this.target.distanceToSqr(point)) <= 1) {
                                this.breath_touches_target = true;
                            }
                        }

                        // inflict damage to target
                        if (this.breath_touches_target) {
                            this.target.hurt(this.tragon.damageSources().dragonBreath(), this.attack_damage);
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
            return !this.attack_is_finished;
        }
    }
}

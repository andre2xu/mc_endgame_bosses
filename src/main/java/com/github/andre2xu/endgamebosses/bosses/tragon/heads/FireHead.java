package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

public class FireHead extends TragonHead {
    public FireHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new Fireballs(parent));
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

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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
                // cancel attack if target doesn't exist, is dead, or is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished;
        }
    }
}

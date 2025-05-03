package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class IceHead extends TragonHead {
    public IceHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new FrostBreath(parent));
    }



    // ATTACKING
    private static class FrostBreath implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private int attack_delay = 0;
        private boolean attack_is_finished = false;

        public FrostBreath(TragonEntity tragon) {
            this.tragon = tragon;
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
            if (this.canAttack()) {
                if (this.attack_delay > 0) {
                    this.decrementAttackDelay();
                }
                else {
                    System.out.println("USING FROST BREATH");

                    this.attack_is_finished = true;
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

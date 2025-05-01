package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FireHead extends TragonHead {
    public FireHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new Fireballs(parent));
    }



    // ATTACKING
    private static class Fireballs implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private boolean attack_is_finished = false;

        public Fireballs(TragonEntity tragon) {
            this.tragon = tragon;
        }

        private boolean canAttack() {
            return this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                System.out.println("Shooting fireballs");
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

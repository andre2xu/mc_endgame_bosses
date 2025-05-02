package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class LightningHead extends TragonHead {
    public LightningHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new LightningStrikes(parent));
    }



    // ATTACKING
    private static class LightningStrikes implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private boolean attack_is_finished = false;

        public LightningStrikes(TragonEntity tragon) {
            this.tragon = tragon;
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(LightningHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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
                System.out.println("Summoning lightning strikes");

                this.attack_is_finished = true;
            }
            else {
                // cancel attack if lightning head is dead, target doesn't exist, target is dead, or target is in creative/spectator mode
                this.attack_is_finished = true;
            }
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished;
        }
    }
}

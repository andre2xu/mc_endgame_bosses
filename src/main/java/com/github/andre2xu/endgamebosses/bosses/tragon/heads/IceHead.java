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
        private boolean attack_is_finished = false;

        public FrostBreath(TragonEntity tragon) {
            this.tragon = tragon;
        }

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(FireHead.class) && this.target != null && this.target.isAlive() && this.tragon.distanceTo(this.target) > 12 && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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

        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished;
        }
    }
}

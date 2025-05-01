package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;

public class FireHead extends TragonHead {
    public FireHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new Fireballs(parent));
    }



    // ATTACKING
    private static class Fireballs implements TragonHeadAttack {
        private final TragonEntity tragon;
        private boolean attack_is_finished = false;

        public Fireballs(TragonEntity tragon) {
            this.tragon = tragon;
        }

        @Override
        public void tick() {
            System.out.println("Shooting fireballs");

            this.attack_is_finished = true;
        }

        @Override
        public boolean canUse() {
            return !this.attack_is_finished;
        }
    }
}

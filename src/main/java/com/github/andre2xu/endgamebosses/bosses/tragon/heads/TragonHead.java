package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;

import java.util.ArrayList;
import java.util.Random;

public class TragonHead {
    private final TragonEntity parent;
    private float health;
    private boolean has_taken_damage = false; // this flag ensures that the Tragon's 'readAdditionalSaveData' method only updates the health of a head if it has already taken damage
    private final ArrayList<TragonHeadAttack> all_attacks = new ArrayList<>();
    private boolean attack_started = false;
    private boolean attack_is_finished = true;
    private TragonHeadAttack attack = null;

    public TragonHead(TragonEntity parent, float maxHealth) {
        this.parent = parent;
        this.health = maxHealth;
    }

    public void setHealth(float newHealth) {
        this.health = newHealth;

        if (this.health <= 0) {
            this.parent.setHeadAliveFlag(this, false); // mark head as dead so that it gets rendered as headless in the model class

            this.health = 0; // prevent health from having a negative value
        }
    }

    public float getHealth() {
        return this.health;
    }

    public void hurt(float damageAmount) {
        if (this.health > 0) {
            this.setHealth(this.health - damageAmount);

            this.has_taken_damage = true;
        }
    }

    public boolean hasTakenDamage() {
        return this.has_taken_damage;
    }

    public void setHasTakenDamage(boolean flag) {
        this.has_taken_damage = flag;
    }



    // ATTACKING
    protected void addAttack(TragonHeadAttack attack) {
        this.all_attacks.add(attack);
    }

    public void chooseAttack() {
        // this method should be called in the 'start' method of a goal since it decides what attack the 'attackTick' method will run

        if (!this.all_attacks.isEmpty()) {
            int index = new Random().nextInt(0, this.all_attacks.size());

            this.attack = this.all_attacks.get(index);

            this.attack_is_finished = false;
        }
    }

    public boolean isFinishedAttacking() {
        // this method should be used to check if 'attackTick' can still be called

        return this.attack_is_finished;
    }

    public void attackTick() {
        // this method should be called in the 'tick' method of a goal. It must be paired with 'isFinishedAttacking', otherwise it will never stop being called

        if (this.attack == null) {
            // mark attack as finished since there's either no attack at all
            this.attack_is_finished = true;
            return;
        }

        if (this.attack.canUse()) {
            // OBJECTIVE: Start attack first (i.e. execute setup code) and then run the attack

            if (!this.attack_started) {
                this.attack.start();
                this.attack_started = true;
            }
            else {
                this.attack.tick();
            }
        }
        else {
            // OBJECTIVE: Stop attack, reset variables, and update flags

            this.attack.stop();
            this.attack = null;

            this.attack_started = false;

            // mark attack as finished since the attack chosen can't run (anymore)
            this.attack_is_finished = true;
        }
    }

    protected interface TragonHeadAttack {
        void resetAttack();

        void start();

        void stop();

        void tick();

        boolean canUse();
    }
}

package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;

public class TragonHead {
    private final TragonEntity parent;
    private float health;
    private boolean is_alive = true;
    private boolean has_taken_damage = false; // this flag ensures that the Tragon's 'readAdditionalSaveData' method only updates the health of a head if it has already taken damage

    public TragonHead(TragonEntity parent, float maxHealth) {
        this.parent = parent;
        this.health = maxHealth;
    }

    public void setHealth(float newHealth) {
        this.health = newHealth;

        if (this.health <= 0) {
            this.is_alive = false;

            this.health = 0; // prevent health from having a negative value
        }
    }

    public float getHealth() {
        return this.health;
    }

    public boolean isAlive() {
        return this.is_alive;
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
}

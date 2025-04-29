package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

public class TragonHead {
    private float health;
    private boolean is_alive = true;

    public TragonHead(float maxHealth) {
        this.health = maxHealth;
    }

    public void setHealth(float newHealth) {
        this.health = newHealth;
    }

    public float getHealth() {
        return this.health;
    }

    public boolean isAlive() {
        return this.is_alive;
    }

    public void hurt(float damageAmount) {
        if (this.health > 0) {
            this.health = this.health - damageAmount;

            if (this.health <= 0) {
                this.is_alive = false;

                this.health = 0; // prevent health from having a negative value
            }
        }
    }
}

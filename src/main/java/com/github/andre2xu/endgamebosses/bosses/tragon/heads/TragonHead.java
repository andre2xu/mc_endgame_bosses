package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.Random;

public class TragonHead {
    private final TragonEntity parent;
    private final String neck_hitbox_id;
    private float health;
    private boolean has_taken_damage = false; // this flag ensures that the Tragon's 'readAdditionalSaveData' method only updates the health of a head if it has already taken damage
    private final ArrayList<TragonHeadAttack> all_attacks = new ArrayList<>();
    private boolean attack_started = false;
    private boolean attack_is_finished = true;
    private TragonHeadAttack attack = null;
    private boolean mouth_is_open = false;

    public TragonHead(TragonEntity parent, float maxHealth, String neckHitboxId) {
        this.parent = parent;
        this.health = maxHealth;
        this.neck_hitbox_id = neckHitboxId;
    }

    public void setHealth(float newHealth) {
        this.health = newHealth;

        if (this.health <= 0) {
            this.parent.setHeadAliveFlag(this, false); // mark head as dead so that it gets rendered as headless in the model class

            this.health = 0; // prevent health from having a negative value

            // play death sound
            this.parent.playHeadDeathSound();

            // show blood particles
            PartEntity<?>[] all_hitboxes = this.parent.getParts();

            if (all_hitboxes != null && this.parent.level() instanceof ServerLevel server_level) {
                for (PartEntity<?> hitbox : all_hitboxes) {
                    if (hitbox instanceof HitboxEntity hitbox_entity && hitbox_entity.getHitboxName().equals(this.neck_hitbox_id)) {
                        Vec3 tragon_look_angle = this.parent.getLookAngle(); // direction Tragon is facing
                        Vec3 spawn_pos = hitbox_entity.position().add(tragon_look_angle.normalize().scale(2)); // slightly in front of neck

                        Vec3 particle_offset = spawn_pos.normalize().multiply(0.5, 0, 0.5); // make blood spurt forward

                        for (int i=0; i < 10; i++) {
                            server_level.sendParticles(
                                    new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.APPLE)), // closest particle to blood is the apple eating crumbs
                                    spawn_pos.x, hitbox_entity.getY() + 3, spawn_pos.z,
                                    5, // particle count
                                    particle_offset.x, particle_offset.y, particle_offset.z,
                                    0.01 // speed
                            );
                        }

                        break;
                    }
                }
            }
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
    public void activatePhase2() {}

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

        if (this.attack != null) {
            if (this.attack.canUse()) {
                // OBJECTIVE: Set up attack, open mouth, then run the real attack tick

                if (!this.attack_started) {
                    this.attack.start(); // runs setup code
                    this.attack_started = true;

                    // open mouth
                    if (!this.mouth_is_open && this.attack.canAttack()) {
                        // noinspection IfCanBeSwitch
                        if (this instanceof FireHead) {
                            this.parent.triggerAnim("fire_head_mouth_movement_trigger_anim_controller", "fire_head_mouth_open");
                        }
                        else if (this instanceof LightningHead) {
                            this.parent.triggerAnim("lightning_head_mouth_movement_trigger_anim_controller", "lightning_head_mouth_open");
                        }
                        else if (this instanceof IceHead) {
                            this.parent.triggerAnim("ice_head_mouth_movement_trigger_anim_controller", "ice_head_mouth_open");
                        }

                        this.mouth_is_open = true;
                    }
                }
                else {
                    this.attack.tick();
                }
            }
            else {
                // OBJECTIVE: Stop attack, reset variables, update flags, and close mouth

                this.attack.stop();
                this.attack = null;

                this.attack_started = false;

                // mark attack as finished since the attack chosen can't run (anymore)
                this.attack_is_finished = true;

                // close mouth
                if (this.mouth_is_open) {
                    // noinspection IfCanBeSwitch
                    if (this instanceof FireHead) {
                        this.parent.triggerAnim("fire_head_mouth_movement_trigger_anim_controller", "fire_head_mouth_close");
                    }
                    else if (this instanceof LightningHead) {
                        this.parent.triggerAnim("lightning_head_mouth_movement_trigger_anim_controller", "lightning_head_mouth_close");
                    }
                    else if (this instanceof IceHead) {
                        this.parent.triggerAnim("ice_head_mouth_movement_trigger_anim_controller", "ice_head_mouth_close");
                    }

                    this.mouth_is_open = false;
                }
            }
        }
    }

    protected interface TragonHeadAttack {
        boolean canAttack();

        void resetAttack();

        void start();

        void stop();

        void tick();

        boolean canUse();
    }
}

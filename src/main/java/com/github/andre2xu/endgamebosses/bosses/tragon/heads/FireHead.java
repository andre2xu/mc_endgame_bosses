package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.misc.HitboxEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

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

        @Override
        public boolean canAttack() {
            return this.tragon != null && this.tragon.getHeadAliveFlag(FireHead.class) && this.target != null && this.target.isAlive() && !(this.target instanceof Player player && (player.isCreative() || player.isSpectator()));
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
                        // OBJECTIVE: Spawn fireball at the fire head's mouth and shoot it at target

                        if (this.tragon.level() instanceof ServerLevel server_level) {
                            Vec3 mouth_pos = this.tragon.getMouthPosition(FireHead.class);
                            Vec3 target_pos = this.target.position();

                            double fireball_spawn_y_offset = 3;

                            Vec3 direction = target_pos.subtract(mouth_pos).add(0, fireball_spawn_y_offset, 0); // from mouth to target
                            Vec3 fireball_spawn_point = mouth_pos.subtract(0, fireball_spawn_y_offset, 0).add(direction.normalize().scale(1)); // adjust the spawn position so that it aligns better with the mouth

                            server_level.addFreshEntity(new CustomFireball(
                                    fireball_spawn_point,
                                    this.tragon,
                                    direction,
                                    server_level
                            ));
                        }

                        // decrease the fireballs counter and start attack cooldown
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



        private static class CustomFireball extends AbstractHurtingProjectile {
            private final LivingEntity owner;

            public CustomFireball(Vec3 origin, LivingEntity pOwner, Vec3 pMovement, Level pLevel) {
                super(EntityType.FIREBALL, origin.x, origin.y, origin.z, pMovement, pLevel);
                this.setOwner(pOwner);
                this.setRot(pOwner.getYRot(), pOwner.getXRot());

                this.owner = pOwner;
            }

            @Override
            protected void onHit(@NotNull HitResult pResult) {
                super.onHit(pResult);

                if (this.level() instanceof ServerLevel server_level) {
                    server_level.explode(
                            this,
                            this.damageSources().mobProjectile(this, this.owner),
                            new CustomExplosionDamageCalculator(), // don't damage the Tragon
                            this.position(), // cause explosion at landing spot
                            3, // radius in blocks
                            server_level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING), // spawn fire
                            Level.ExplosionInteraction.MOB
                    );

                    // delete fireball in game
                    this.discard();
                }
            }

            private static class CustomExplosionDamageCalculator extends ExplosionDamageCalculator {
                @Override
                public boolean shouldDamageEntity(@NotNull Explosion pExplosion, @NotNull Entity pEntity) {
                    if (pEntity instanceof TragonEntity || pEntity instanceof HitboxEntity) {
                        return false;
                    }

                    return super.shouldDamageEntity(pExplosion, pEntity);
                }
            }
        }
    }
}

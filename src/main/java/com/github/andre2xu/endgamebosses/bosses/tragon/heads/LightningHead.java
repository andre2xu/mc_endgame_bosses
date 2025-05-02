package com.github.andre2xu.endgamebosses.bosses.tragon.heads;

import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class LightningHead extends TragonHead {
    public LightningHead(TragonEntity parent, float maxHealth) {
        super(parent, maxHealth);

        this.addAttack(new LightningStrikes(parent));
    }



    // ATTACKING
    private static class LightningStrikes implements TragonHeadAttack {
        private final TragonEntity tragon;
        private LivingEntity target = null;
        private int num_of_lightning_strikes_to_summon = 0;
        private boolean first_strike_summoned = false;
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
            this.num_of_lightning_strikes_to_summon = 0;
            this.first_strike_summoned = false;
            this.attack_is_finished = false;
        }

        @Override
        public void start() {
            // save a reference of the target to avoid having to call 'this.tragon.getTarget' which can sometimes return null
            this.target = this.tragon.getTarget();

            // pick a random number of lightning strikes to summon
            this.num_of_lightning_strikes_to_summon = new Random().nextInt(4, 6); // 4 to 5
        }

        @Override
        public void stop() {
            this.resetAttack();
        }

        @Override
        public void tick() {
            if (this.canAttack()) {
                // make mouth of lightning head glow
                Vec3 mouth_pos = this.tragon.getMouthPosition(LightningHead.class);
                Vec3 vector_to_target = this.target.position().subtract(mouth_pos).normalize().scale(1.5);

                mouth_pos = mouth_pos.add(vector_to_target.x, 0, vector_to_target.z); // spawn position is in front of mouth

                if (this.tragon.level() instanceof ServerLevel server_level) {
                    server_level.sendParticles(
                            ParticleTypes.END_ROD,
                            mouth_pos.x, mouth_pos.y - 3, mouth_pos.z,
                            5, // particle count
                            0, 0, 0,
                            0.04 // speed
                    );

                    // summon lightning strikes
                    if (this.num_of_lightning_strikes_to_summon > 0) {
                        if (!this.first_strike_summoned) {
                            // summon directly at target's position
                            LightningBolt first_strike = EntityType.LIGHTNING_BOLT.create(
                                    server_level,
                                    null,
                                    this.target.blockPosition(),
                                    MobSpawnType.EVENT,
                                    false,
                                    false
                            );

                            if (first_strike != null) {
                                server_level.addFreshEntity(first_strike);
                            }

                            this.first_strike_summoned = true;
                        }
                        else {
                            System.out.println(this.num_of_lightning_strikes_to_summon);
                        }

                        this.num_of_lightning_strikes_to_summon--;
                    }
                    else {
                        // stop attack
                        this.attack_is_finished = true;
                    }
                }
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

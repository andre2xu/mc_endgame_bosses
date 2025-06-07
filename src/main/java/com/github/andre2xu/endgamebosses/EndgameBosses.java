package com.github.andre2xu.endgamebosses;

import com.github.andre2xu.endgamebosses.bosses.BossRegistry;
import com.github.andre2xu.endgamebosses.bosses.LootModifierRegistry;
import com.github.andre2xu.endgamebosses.bosses.MiscEntityRegistry;
import com.github.andre2xu.endgamebosses.bosses.ProjectilesRegistry;
import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.MamaRenderer;
import com.github.andre2xu.endgamebosses.bosses.mama.egg_sac.MamaEggSacEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.egg_sac.MamaEggSacRenderer;
import com.github.andre2xu.endgamebosses.bosses.mama.spiderling.SpiderlingEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.spiderling.SpiderlingRenderer;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonEntity;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonRenderer;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.missile.MechalodonMissileEntity;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.missile.MechalodonMissileRenderer;
import com.github.andre2xu.endgamebosses.bosses.samurice.SamuriceEntity;
import com.github.andre2xu.endgamebosses.bosses.samurice.SamuriceRenderer;
import com.github.andre2xu.endgamebosses.bosses.samurice.clone.SamuriceCloneEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonRenderer;
import com.github.andre2xu.endgamebosses.bosses.tragon.icicle.TragonIcicleEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.icicle.TragonIcicleRenderer;
import com.github.andre2xu.endgamebosses.data.BossStateData;
import com.github.andre2xu.endgamebosses.networking.MainChannel;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EndgameBosses.MODID)
public class EndgameBosses {
    public static final String MODID = "endgamebosses";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String[] BOSSES = {
            "mechalodon",
            "tragon",
            "mama",
            "samurice"
    };



    public EndgameBosses() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // register boss types
        BossRegistry.register(modEventBus);

        // register custom projectile types
        ProjectilesRegistry.register(modEventBus);

        // register other entity types
        MiscEntityRegistry.register(modEventBus);

        // register loot modifier serializers
        LootModifierRegistry.register(modEventBus);

        // register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}



    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        ResourceKey<Level> level = event.getTo();

        // if a player goes to the End, check if the Ender Dragon has already been killed before. This is for players who have already killed the Ender Dragon before installing the mod
        if (level.equals(Level.END)) {
            Player player = event.getEntity();

            if (player.level() instanceof ServerLevel serverLevel) {
                EndDragonFight dragon_fight = serverLevel.getDragonFight();

                if (dragon_fight != null && dragon_fight.hasPreviouslyKilledDragon()) {
                    MinecraftServer server = player.getServer();

                    if (server != null) {
                        // get data storage
                        BossStateData boss_state_data = BossStateData.createOrGet(server);
                        String boss_name = "ender_dragon";

                        if (boss_state_data.isBossAlive(boss_name)) {
                            // mark the Ender Dragon as dead (set persistent flag)
                            boss_state_data.setBossState(boss_name, BossStateData.State.DEAD);

                            // add new bosses
                            boss_state_data.addBosses(BOSSES);

                            // send a message to all players
                            for (Player p : serverLevel.players()) {
                                p.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.dragon_death")); // see lang folder in resources
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityDeath(final LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // check if the entity that died is an Ender Dragon and if it was killed by a player
        if (entity.getType() == EntityType.ENDER_DRAGON && entity.level() instanceof ServerLevel serverLevel) {
            Entity killer = event.getSource().getEntity();

            if (killer != null && killer.getType() == EntityType.PLAYER) {
                MinecraftServer server = killer.getServer();

                if (server != null) {
                    // get data storage
                    BossStateData boss_state_data = BossStateData.createOrGet(server);
                    String boss_name = "ender_dragon";

                    if (boss_state_data.isBossAlive(boss_name)) {
                        // mark the Ender Dragon as dead (set persistent flag)
                        boss_state_data.setBossState(boss_name, BossStateData.State.DEAD);

                        // add new bosses
                        boss_state_data.addBosses(BOSSES);

                        // send a message to all players
                        for (Player p : serverLevel.players()) {
                            p.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.dragon_death")); // see lang folder in resources
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();

        if (server != null) {
            // decrement boss spawn cooldown (if there's an active boss)
            BossStateData boss_state_data = BossStateData.createOrGet(server);

            boss_state_data.updateBossSpawnCooldown();
        }
    }

    @SubscribeEvent
    public void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        // prevent double execution
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // handle boss spawning
        MinecraftServer player_server = event.player.getServer();

        if (player_server != null) {
            BossStateData boss_state_data = BossStateData.createOrGet(player_server);

            // check what biome a player is in every 10 seconds
            if (!boss_state_data.isBossAlive("ender_dragon") && event.player.tickCount % 200 == 0 && event.player.level() instanceof ServerLevel server_level) {
                String active_boss = boss_state_data.getActiveBoss();

                if (active_boss.isEmpty()) {
                    // OBJECTIVE: Spawn the appropriate boss for the player's biome and set them as the active one. Do not let another boss spawn until the active one is killed or the variable is reset (see the BossStateData class)

                    BlockPos player_block_pos = BlockPos.containing(event.player.position());
                    Holder<Biome> player_biome = server_level.getBiome(player_block_pos);

                    if (player_biome.is(Tags.Biomes.IS_DESERT) && boss_state_data.isBossAlive("mechalodon")) {
                        boss_state_data.setActiveBoss("mechalodon");
                    }
                    else if (player_biome.is(BiomeTags.IS_OCEAN) && boss_state_data.isBossAlive("tragon")) {
                        boss_state_data.setActiveBoss("tragon");
                    }
                    else if (player_biome.is(Tags.Biomes.IS_SNOWY) && boss_state_data.isBossAlive("samurice")) {
                        boss_state_data.setActiveBoss("samurice");
                    }

                    // NOTE: See MamaEggSacEntity::hurt & MamaEggSacEntity::die for Mama's spawning mechanism
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {}

        @SubscribeEvent
        public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            // bosses
            event.registerEntityRenderer(BossRegistry.MECHALODON.get(), MechalodonRenderer::new);
            event.registerEntityRenderer(BossRegistry.TRAGON.get(), TragonRenderer::new);
            event.registerEntityRenderer(BossRegistry.MAMA.get(), MamaRenderer::new);
            event.registerEntityRenderer(BossRegistry.SAMURICE.get(), SamuriceRenderer::new);

            // misc
            event.registerEntityRenderer(MiscEntityRegistry.MAMA_EGG_SAC.get(), MamaEggSacRenderer::new);
            event.registerEntityRenderer(MiscEntityRegistry.SPIDERLING.get(), SpiderlingRenderer::new);
            event.registerEntityRenderer(MiscEntityRegistry.SAMURICE_CLONE.get(), SamuriceRenderer::new);

            // projectiles
            event.registerEntityRenderer(ProjectilesRegistry.MECHALODON_MISSILE.get(), MechalodonMissileRenderer::new);
            event.registerEntityRenderer(ProjectilesRegistry.TRAGON_ICICLE.get(), TragonIcicleRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeModEvents {
        @SubscribeEvent
        public static void onMobSpawn(final MobSpawnEvent.FinalizeSpawn event) {
            Entity entity = event.getEntity();

            // prevent boss spawn on peaceful worlds
            if (entity.getCommandSenderWorld().getDifficulty() == Difficulty.PEACEFUL && (entity instanceof MechalodonEntity || entity instanceof TragonEntity || entity instanceof MamaEntity || entity instanceof SamuriceEntity)) {
                event.setSpawnCancelled(true);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class GeneralModEvents {
        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                MainChannel.registerPacketsToProcess();
            });
        }

        @SubscribeEvent
        public static void createEntityAttributes(EntityAttributeCreationEvent event) {
            // bosses
            event.put(BossRegistry.MECHALODON.get(), MechalodonEntity.createAttributes());
            event.put(BossRegistry.TRAGON.get(), TragonEntity.createAttributes());
            event.put(BossRegistry.MAMA.get(), MamaEntity.createAttributes());
            event.put(BossRegistry.SAMURICE.get(), SamuriceEntity.createAttributes());

            // misc
            event.put(MiscEntityRegistry.MAMA_EGG_SAC.get(), MamaEggSacEntity.createAttributes());
            event.put(MiscEntityRegistry.SPIDERLING.get(), SpiderlingEntity.createAttributes());
            event.put(MiscEntityRegistry.SAMURICE_CLONE.get(), SamuriceCloneEntity.createAttributes());

            // projectiles
            event.put(ProjectilesRegistry.MECHALODON_MISSILE.get(), MechalodonMissileEntity.createAttributes());
            event.put(ProjectilesRegistry.TRAGON_ICICLE.get(), TragonIcicleEntity.createAttributes());
        }
    }
}

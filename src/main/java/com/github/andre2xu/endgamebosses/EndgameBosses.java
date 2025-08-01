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
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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

import java.util.List;

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

        if (entity.level() instanceof ServerLevel server_level) {
            if (entity.getType() == EntityType.ENDER_DRAGON) {
                // check if the entity that died is an Ender Dragon and if it was killed by a player

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
                            for (Player p : server_level.players()) {
                                p.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.dragon_death")); // see lang folder in resources
                            }
                        }
                    }
                }
            }
            else {
                boolean play_victory_effects = false;
                String boss_name = "";

                //noinspection IfCanBeSwitch
                if (entity instanceof MechalodonEntity) {
                    boss_name = "Mechalodon";
                    play_victory_effects = true;
                }
                else if (entity instanceof TragonEntity) {
                    boss_name = "Tragon";
                    play_victory_effects = true;
                }
                else if (entity instanceof MamaEntity) {
                    boss_name = "Mama";
                    play_victory_effects = true;
                }
                else if (entity instanceof SamuriceEntity) {
                    boss_name = "Samurice";
                    play_victory_effects = true;
                }

                if (play_victory_effects) {
                    List<ServerPlayer> all_players = server_level.players();

                    // tell clients to display the victory message
                    for (ServerPlayer player : all_players) {
                        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e§o" + boss_name)));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("endgamebosses.msg.victory_subtitle")));
                    }

                    // tell clients to play the victory sound
                    server_level.playSound(
                            null, // all players
                            BlockPos.containing(entity.position()),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                            SoundSource.PLAYERS,
                            1f, 1f
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(final EntityJoinLevelEvent event) {
        // check if the Ender Dragon was revived
        if (event.getEntity() instanceof EnderDragon ender_dragon && ender_dragon.level().dimension() == Level.END) {
            MinecraftServer server = ender_dragon.getServer();

            if (server != null) {
                BossStateData boss_state_data = BossStateData.createOrGet(server);

                // mark Ender Dragon as alive and delete the states of other bosses (so they can be re-added when the dragon is killed again)
                boss_state_data.reset();
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

                        // alert nearby players of boss spawn
                        List<Player> nearby_players = server_level.getEntitiesOfClass(Player.class, event.player.getBoundingBox().inflate(60));

                        for (Player player : nearby_players) {
                            player.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.mechalodon_spawn_alert"));
                        }

                        // spawn Mechalodon
                        MechalodonEntity mechalodon = BossRegistry.MECHALODON.get().create(server_level);

                        if (mechalodon != null) {
                            Vec3 player_pos = event.player.position();

                            mechalodon.setPos(new Vec3(
                                    player_pos.x,
                                    player_pos.y - 20, // appear below the player
                                    player_pos.z
                            ));

                            server_level.addFreshEntity(mechalodon);
                        }
                    }
                    else if (player_biome.is(BiomeTags.IS_OCEAN) && boss_state_data.isBossAlive("tragon")) {
                        boss_state_data.setActiveBoss("tragon");

                        // alert nearby players of boss spawn
                        List<Player> nearby_players = server_level.getEntitiesOfClass(Player.class, event.player.getBoundingBox().inflate(60));

                        for (Player player : nearby_players) {
                            player.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.tragon_spawn_alert"));
                        }

                        // spawn Tragon
                        TragonEntity tragon = BossRegistry.TRAGON.get().create(server_level);

                        if (tragon != null) {
                            Vec3 player_pos = event.player.position();

                            tragon.setPos(new Vec3(
                                    player_pos.x,
                                    player_pos.y + 30, // appear above the player
                                    player_pos.z
                            ));

                            server_level.addFreshEntity(tragon);
                        }
                    }
                    else if (player_biome.is(Tags.Biomes.IS_SNOWY) && boss_state_data.isBossAlive("samurice")) {
                        boss_state_data.setActiveBoss("samurice");

                        // alert nearby players of boss spawn
                        List<Player> nearby_players = server_level.getEntitiesOfClass(Player.class, event.player.getBoundingBox().inflate(40));

                        for (Player player : nearby_players) {
                            player.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.samurice_spawn_alert"));
                        }

                        // spawn Samurice
                        SamuriceEntity samurice = BossRegistry.SAMURICE.get().create(server_level);

                        if (samurice != null) {
                            Vec3 player_pos = event.player.position();

                            samurice.setPos(new Vec3(
                                    player_pos.x,
                                    player_pos.y + 3, // appear above the player
                                    player_pos.z
                            ));

                            server_level.addFreshEntity(samurice);
                        }
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

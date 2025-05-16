package com.github.andre2xu.endgamebosses;

import com.github.andre2xu.endgamebosses.bosses.BossRegistry;
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
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonRenderer;
import com.github.andre2xu.endgamebosses.bosses.tragon.icicle.TragonIcicleEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.icicle.TragonIcicleRenderer;
import com.github.andre2xu.endgamebosses.data.BossStateData;
import com.github.andre2xu.endgamebosses.networking.MainChannel;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
            "mama"
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
                                p.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.dragondeath")); // see lang folder in resources
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
                            p.sendSystemMessage(Component.translatable("endgamebosses.sysmsg.dragondeath")); // see lang folder in resources
                        }
                    }
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

            // misc
            event.registerEntityRenderer(MiscEntityRegistry.MAMA_EGG_SAC.get(), MamaEggSacRenderer::new);
            event.registerEntityRenderer(MiscEntityRegistry.SPIDERLING.get(), SpiderlingRenderer::new);

            // projectiles
            event.registerEntityRenderer(ProjectilesRegistry.MECHALODON_MISSILE.get(), MechalodonMissileRenderer::new);
            event.registerEntityRenderer(ProjectilesRegistry.TRAGON_ICICLE.get(), TragonIcicleRenderer::new);
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

            // misc
            event.put(MiscEntityRegistry.MAMA_EGG_SAC.get(), MamaEggSacEntity.createAttributes());
            event.put(MiscEntityRegistry.SPIDERLING.get(), SpiderlingEntity.createAttributes());

            // projectiles
            event.put(ProjectilesRegistry.MECHALODON_MISSILE.get(), MechalodonMissileEntity.createAttributes());
            event.put(ProjectilesRegistry.TRAGON_ICICLE.get(), TragonIcicleEntity.createAttributes());
        }
    }
}

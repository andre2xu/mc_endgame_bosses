package com.github.andre2xu.endgamebosses;

import com.github.andre2xu.endgamebosses.bosses.BossRegistry;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonEntity;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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



    public EndgameBosses() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // register boss types
        BossRegistry.register(modEventBus);

        // register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}



    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        ResourceKey<Level> level = event.getTo();

        // if a player goes to the End, check if the Ender Dragon has already been killed before
        if (level.equals(Level.END)) {
            Player player = event.getEntity();

            if (player.level() instanceof ServerLevel serverLevel) {
                EndDragonFight dragon_fight = serverLevel.getDragonFight();

                if (dragon_fight != null && dragon_fight.hasPreviouslyKilledDragon()) {
                    System.out.println("Endgame bosses can now be fought");
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityDeath(final LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getType() == EntityType.ENDER_DRAGON) {
            System.out.println("Endgame bosses can now be fought");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {}

        @SubscribeEvent
        public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(BossRegistry.MECHALODON.get(), MechalodonRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class GeneralModEvents {
        @SubscribeEvent
        public static void createEntityAttributes(EntityAttributeCreationEvent event) {
            event.put(BossRegistry.MECHALODON.get(), MechalodonEntity.createAttributes());
        }
    }
}

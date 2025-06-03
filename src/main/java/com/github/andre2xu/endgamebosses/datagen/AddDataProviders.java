package com.github.andre2xu.endgamebosses.datagen;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = EndgameBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AddDataProviders {
    @SubscribeEvent
    public static void gatherDataProviders(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput pack_output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup_provider = event.getLookupProvider();

        // global loot modifiers
        generator.addProvider(event.includeServer(), new ModGlobalLootModifierProvider(pack_output, lookup_provider));
    }
}

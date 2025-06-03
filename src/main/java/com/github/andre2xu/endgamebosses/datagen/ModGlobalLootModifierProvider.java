package com.github.andre2xu.endgamebosses.datagen;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.loot.BossLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    // NOTE: For this to work for an entity, it must have a loot table JSON file. Please see resources/data/endgamebosses/loot_table/entities

    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, EndgameBosses.MODID, registries);
    }

    private void addBossDrops(String bossRegistryName, BossLoot[] drops) {
        ResourceLocation loot_table_file_path = ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "entities/" + bossRegistryName); // points to a JSON file in resources/data/endgamebosses/loot_table/entities

        for (BossLoot drop : drops) {
            BossLootModifier new_drop = new BossLootModifier(new LootItemCondition[] {
                    new LootTableIdCondition.Builder(loot_table_file_path)
                            .and(LootItemRandomChanceCondition.randomChance(drop.probability))
                            .build()
            }, drop.item, drop.min, drop.max);

            // create loot modifier JSON file
            this.add(bossRegistryName + "_drop_" + drop.name, new_drop);
        }
    }

    @Override
    protected void start(HolderLookup.@NotNull Provider registries) {

    }



    private record BossLoot(String name, ItemStack item, float probability, int min, int max) {
    }
}

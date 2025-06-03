package com.github.andre2xu.endgamebosses.datagen;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.loot.BossLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
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

    private ItemStack createEnchantedBook(HolderLookup.@NotNull Provider registries, CustomEnchantment[] enchantments) {
        ItemStack enchanted_book = new ItemStack(Items.ENCHANTED_BOOK);

        // add enchantments
        for (CustomEnchantment enchantment : enchantments) {
            registries.lookup(Registries.ENCHANTMENT).flatMap(action -> action.get(enchantment.name)).ifPresent(action2 -> enchanted_book.enchant(action2, enchantment.level));
        }

        return enchanted_book;
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
        this.addBossDrops("mechalodon", new BossLoot[] {
                new BossLoot("ancient_debris", new ItemStack(Items.ANCIENT_DEBRIS), 1, 1, 4),
                new BossLoot("iron", new ItemStack(Items.IRON_INGOT), 1, 8, 64),
                new BossLoot("redstone", new ItemStack(Items.REDSTONE), 1, 32, 64),
                new BossLoot("emerald", new ItemStack(Items.EMERALD), 0.7f, 12, 32),
                new BossLoot("gold", new ItemStack(Items.GOLD_INGOT), 0.7f, 12, 32),
                new BossLoot("diamond", new ItemStack(Items.DIAMOND), 0.5f, 6, 20),
                new BossLoot("wither_skeleton_skull", new ItemStack(Items.WITHER_SKELETON_SKULL), 0.3f, 1, 2),
                new BossLoot("mending", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MENDING, 1)
                }), 0.1f, 1, 1)
        });
    }



    private record BossLoot(String name, ItemStack item, float probability, int min, int max) {}

    private record CustomEnchantment(ResourceKey<Enchantment> name, int level) {}
}

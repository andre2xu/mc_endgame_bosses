package com.github.andre2xu.endgamebosses.datagen;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.loot.BossLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
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
                new BossLoot("iron_sword", new ItemStack(Items.IRON_SWORD), 0.4f, 1, 1),
                new BossLoot("iron_pickaxe", new ItemStack(Items.IRON_PICKAXE), 0.4f, 1, 1),
                new BossLoot("iron_shovel", new ItemStack(Items.IRON_SHOVEL), 0.4f, 1, 1),
                new BossLoot("iron_axe", new ItemStack(Items.IRON_AXE), 0.4f, 1, 1),
                new BossLoot("iron_helmet", new ItemStack(Items.IRON_HELMET), 0.3f, 1, 1),
                new BossLoot("iron_chestplate", new ItemStack(Items.IRON_CHESTPLATE), 0.3f, 1, 1),
                new BossLoot("iron_leggings", new ItemStack(Items.IRON_LEGGINGS), 0.3f, 1, 1),
                new BossLoot("iron_boots", new ItemStack(Items.IRON_BOOTS), 0.3f, 1, 1),
                new BossLoot("wither_skeleton_skull", new ItemStack(Items.WITHER_SKELETON_SKULL), 0.3f, 1, 2),
                new BossLoot("unbreaking_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 2)
                }), 0.2f, 1, 1),
                new BossLoot("blast_protection_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BLAST_PROTECTION, 3)
                }), 0.2f, 1, 1),
                new BossLoot("nether_star", new ItemStack(Items.NETHER_STAR), 0.1f, 1, 1),
                new BossLoot("mending", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MENDING, 1)
                }), 0.1f, 1, 1),
                new BossLoot("unbreaking_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 3)
                }), 0.1f, 1, 1),
                new BossLoot("blast_protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BLAST_PROTECTION, 4)
                }), 0.1f, 1, 1),
                new BossLoot("channeling", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.CHANNELING, 1)
                }), 0.1f, 1, 1),
        });
    }



    private record BossLoot(String name, ItemStack item, float probability, int min, int max) {}

    private record CustomEnchantment(ResourceKey<Enchantment> name, int level) {}
}

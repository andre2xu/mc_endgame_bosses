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
        // MECHALODON
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
                new BossLoot("protection_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 3)
                }), 0.2f, 1, 1),
                new BossLoot("protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 4)
                }), 0.1f, 1, 1),
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
                }), 0.1f, 1, 1)
        });

        // TRAGON
        this.addBossDrops("tragon", new BossLoot[] {
                new BossLoot("ancient_debris", new ItemStack(Items.ANCIENT_DEBRIS), 1, 2, 6),
                new BossLoot("iron", new ItemStack(Items.IRON_INGOT), 1, 16, 32),
                new BossLoot("diamond", new ItemStack(Items.DIAMOND), 0.8f, 16, 64),
                new BossLoot("gold", new ItemStack(Items.GOLD_INGOT), 0.7f, 16, 64),
                new BossLoot("sea_grass", new ItemStack(Items.SEAGRASS), 0.7f, 6, 12),
                new BossLoot("sea_pickle", new ItemStack(Items.SEA_PICKLE), 0.7f, 1, 6),
                new BossLoot("kelp", new ItemStack(Items.KELP), 0.7f, 8, 32),
                new BossLoot("tropical_fish", new ItemStack(Items.TROPICAL_FISH), 0.7f, 4, 64),
                new BossLoot("salmon", new ItemStack(Items.SALMON), 0.7f, 4, 64),
                new BossLoot("turtle_scute", new ItemStack(Items.TURTLE_SCUTE), 0.6f, 3, 12),
                new BossLoot("fire_charge", new ItemStack(Items.FIRE_CHARGE), 0.6f, 1, 4),
                new BossLoot("rotten_flesh", new ItemStack(Items.ROTTEN_FLESH), 0.6f, 12, 64),
                new BossLoot("emerald", new ItemStack(Items.EMERALD), 0.5f, 16, 64),
                new BossLoot("turtle_egg", new ItemStack(Items.TURTLE_EGG), 0.5f, 2, 8),
                new BossLoot("blue_ice", new ItemStack(Items.BLUE_ICE), 0.4f, 10, 20),
                new BossLoot("diamond_sword", new ItemStack(Items.DIAMOND_SWORD), 0.4f, 1, 1),
                new BossLoot("diamond_pickaxe", new ItemStack(Items.DIAMOND_PICKAXE), 0.4f, 1, 1),
                new BossLoot("diamond_shovel", new ItemStack(Items.DIAMOND_SHOVEL), 0.4f, 1, 1),
                new BossLoot("diamond_axe", new ItemStack(Items.DIAMOND_AXE), 0.4f, 1, 1),
                new BossLoot("trident", new ItemStack(Items.TRIDENT), 0.4f, 1, 1),
                new BossLoot("nautilus_shell", new ItemStack(Items.NAUTILUS_SHELL), 0.4f, 2, 6),
                new BossLoot("diamond_helmet", new ItemStack(Items.DIAMOND_HELMET), 0.3f, 1, 1),
                new BossLoot("diamond_chestplate", new ItemStack(Items.DIAMOND_CHESTPLATE), 0.3f, 1, 1),
                new BossLoot("diamond_leggings", new ItemStack(Items.DIAMOND_LEGGINGS), 0.3f, 1, 1),
                new BossLoot("diamond_boots", new ItemStack(Items.DIAMOND_BOOTS), 0.3f, 1, 1),
                new BossLoot("heart_of_the_sea", new ItemStack(Items.HEART_OF_THE_SEA), 0.3f, 1, 1),
                new BossLoot("mending", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MENDING, 1)
                }), 0.3f, 1, 1),
                new BossLoot("unbreaking_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 3)
                }), 0.3f, 1, 1),
                new BossLoot("aqua_affinity", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.AQUA_AFFINITY, 1)
                }), 0.3f, 1, 1),
                new BossLoot("blast_protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BLAST_PROTECTION, 4)
                }), 0.3f, 1, 1),
                new BossLoot("protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 4)
                }), 0.3f, 1, 1),
                new BossLoot("depth_strider_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.DEPTH_STRIDER, 2)
                }), 0.3f, 1, 1),
                new BossLoot("fire_protection_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FIRE_PROTECTION, 3)
                }), 0.3f, 1, 1),
                new BossLoot("projectile_protection_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROJECTILE_PROTECTION, 3)
                }), 0.3f, 1, 1),
                new BossLoot("respiration_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.RESPIRATION, 3)
                }), 0.3f, 1, 1),
                new BossLoot("fire_aspect_1", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FIRE_ASPECT, 1)
                }), 0.3f, 1, 1),
                new BossLoot("projectile_protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROJECTILE_PROTECTION, 4)
                }), 0.2f, 1, 1),
                new BossLoot("depth_strider_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.DEPTH_STRIDER, 3)
                }), 0.2f, 1, 1),
                new BossLoot("fire_protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FIRE_PROTECTION, 4)
                }), 0.2f, 1, 1),
                new BossLoot("frost_walker_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FROST_WALKER, 2)
                }), 0.2f, 1, 1),
                new BossLoot("fire_aspect_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FIRE_ASPECT, 2)
                }), 0.2f, 1, 1),
                new BossLoot("sharpness_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 3)
                }), 0.2f, 1, 1),
                new BossLoot("flame", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FLAME, 1)
                }), 0.2f, 1, 1),
                new BossLoot("infinity", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.INFINITY, 1)
                }), 0.2f, 1, 1),
                new BossLoot("power_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.POWER, 3)
                }), 0.2f, 1, 1),
                new BossLoot("netherite_sword", new ItemStack(Items.NETHERITE_SWORD), 0.2f, 1, 1),
                new BossLoot("netherite_pickaxe", new ItemStack(Items.NETHERITE_PICKAXE), 0.2f, 1, 1),
                new BossLoot("netherite_helmet", new ItemStack(Items.NETHERITE_HELMET), 0.1f, 1, 1),
                new BossLoot("netherite_chestplate", new ItemStack(Items.NETHERITE_CHESTPLATE), 0.1f, 1, 1),
                new BossLoot("netherite_leggings", new ItemStack(Items.NETHERITE_LEGGINGS), 0.1f, 1, 1),
                new BossLoot("netherite_boots", new ItemStack(Items.NETHERITE_BOOTS), 0.1f, 1, 1),
                new BossLoot("nether_star", new ItemStack(Items.NETHER_STAR), 0.1f, 1, 1),
                new BossLoot("looting_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LOOTING, 3)
                }), 0.1f, 1, 1),
                new BossLoot("thorns_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.THORNS, 3)
                }), 0.1f, 1, 1),
                new BossLoot("breach_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BREACH, 3)
                }), 0.1f, 1, 1),
                new BossLoot("sharpness_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 4)
                }), 0.1f, 1, 1),
                new BossLoot("power_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.POWER, 4)
                }), 0.1f, 1, 1),
                new BossLoot("punch_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PUNCH, 2)
                }), 0.1f, 1, 1),
                new BossLoot("luck_of_the_sea_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LUCK_OF_THE_SEA, 2)
                }), 0.1f, 1, 1),
                new BossLoot("lure_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LURE, 2)
                }), 0.1f, 1, 1),
                new BossLoot("luck_of_the_sea_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LUCK_OF_THE_SEA, 3)
                }), 0.05f, 1, 1),
                new BossLoot("lure_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LURE, 3)
                }), 0.05f, 1, 1),
                new BossLoot("power_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.POWER, 5)
                }), 0.05f, 1, 1),
                new BossLoot("breach_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BREACH, 4)
                }), 0.05f, 1, 1),
                new BossLoot("sharpness_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 5)
                }), 0.05f, 1, 1),
                new BossLoot("fortune_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FORTUNE, 3)
                }), 0.05f, 1, 1)
        });

        // MAMA
        this.addBossDrops("mama", new BossLoot[] {
                new BossLoot("ancient_debris", new ItemStack(Items.ANCIENT_DEBRIS), 1, 2, 4),
                new BossLoot("iron", new ItemStack(Items.IRON_INGOT), 0.8f, 8, 32),
                new BossLoot("gold", new ItemStack(Items.GOLD_INGOT), 0.7f, 8, 16),
                new BossLoot("diamond", new ItemStack(Items.DIAMOND), 0.7f, 16, 32),
                new BossLoot("spider_eye", new ItemStack(Items.SPIDER_EYE), 0.7f, 1, 6),
                new BossLoot("cobweb", new ItemStack(Items.COBWEB), 0.7f, 4, 20),
                new BossLoot("rotten_flesh", new ItemStack(Items.ROTTEN_FLESH), 0.7f, 8, 64),
                new BossLoot("emerald", new ItemStack(Items.EMERALD), 0.6f, 16, 64),
                new BossLoot("bane_of_arthropods_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BANE_OF_ARTHROPODS, 4)
                }), 0.6f, 1, 1),
                new BossLoot("silk_touch", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SILK_TOUCH, 1)
                }), 0.6f, 1, 1),
                new BossLoot("diamond_sword", new ItemStack(Items.DIAMOND_SWORD), 0.5f, 1, 1),
                new BossLoot("diamond_pickaxe", new ItemStack(Items.DIAMOND_PICKAXE), 0.5f, 1, 1),
                new BossLoot("diamond_shovel", new ItemStack(Items.DIAMOND_SHOVEL), 0.5f, 1, 1),
                new BossLoot("diamond_axe", new ItemStack(Items.DIAMOND_AXE), 0.5f, 1, 1),
                new BossLoot("diamond_helmet", new ItemStack(Items.DIAMOND_HELMET), 0.5f, 1, 1),
                new BossLoot("diamond_chestplate", new ItemStack(Items.DIAMOND_CHESTPLATE), 0.4f, 1, 1),
                new BossLoot("diamond_leggings", new ItemStack(Items.DIAMOND_LEGGINGS), 0.4f, 1, 1),
                new BossLoot("diamond_boots", new ItemStack(Items.DIAMOND_BOOTS), 0.4f, 1, 1),
                new BossLoot("bane_of_arthropods_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.BANE_OF_ARTHROPODS, 5)
                }), 0.4f, 1, 1),
                new BossLoot("mending", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MENDING, 1)
                }), 0.4f, 1, 1),
                new BossLoot("looting_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LOOTING, 2)
                }), 0.4f, 1, 1),
                new BossLoot("feather_falling_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FEATHER_FALLING, 3)
                }), 0.4f, 1, 1),
                new BossLoot("protection_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 3)
                }), 0.4f, 1, 1),
                new BossLoot("thorns_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.THORNS, 3)
                }), 0.4f, 1, 1),
                new BossLoot("knockback_1", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.KNOCKBACK, 1)
                }), 0.4f, 1, 1),
                new BossLoot("sweeping_edge_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SWEEPING_EDGE, 2)
                }), 0.4f, 1, 1),
                new BossLoot("feather_falling_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FEATHER_FALLING, 4)
                }), 0.3f, 1, 1),
                new BossLoot("sweeping_edge_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SWEEPING_EDGE, 3)
                }), 0.3f, 1, 1),
                new BossLoot("looting_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LOOTING, 3)
                }), 0.3f, 1, 1),
                new BossLoot("protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 4)
                }), 0.3f, 1, 1),
                new BossLoot("thorns_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.THORNS, 4)
                }), 0.3f, 1, 1),
                new BossLoot("fire_aspect_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FIRE_ASPECT, 2)
                }), 0.3f, 1, 1),
                new BossLoot("unbreaking_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 2)
                }), 0.3f, 1, 1),
                new BossLoot("flame", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FLAME, 1)
                }), 0.3f, 1, 1),
                new BossLoot("infinity", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.INFINITY, 1)
                }), 0.3f, 1, 1),
                new BossLoot("multishot", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MULTISHOT, 1)
                }), 0.3f, 1, 1),
                new BossLoot("quick_charge_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.QUICK_CHARGE, 2)
                }), 0.3f, 1, 1),
                new BossLoot("netherite_sword", new ItemStack(Items.NETHERITE_SWORD), 0.3f, 1, 1),
                new BossLoot("netherite_pickaxe", new ItemStack(Items.NETHERITE_PICKAXE), 0.3f, 1, 1),
                new BossLoot("netherite_helmet", new ItemStack(Items.NETHERITE_HELMET), 0.3f, 1, 1),
                new BossLoot("netherite_chestplate", new ItemStack(Items.NETHERITE_CHESTPLATE), 0.2f, 1, 1),
                new BossLoot("netherite_leggings", new ItemStack(Items.NETHERITE_LEGGINGS), 0.2f, 1, 1),
                new BossLoot("netherite_boots", new ItemStack(Items.NETHERITE_BOOTS), 0.2f, 1, 1),
                new BossLoot("unbreaking_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 3)
                }), 0.2f, 1, 1),
                new BossLoot("sharpness_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 4)
                }), 0.2f, 1, 1),
                new BossLoot("piercing_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PIERCING, 3)
                }), 0.2f, 1, 1),
                new BossLoot("power_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.POWER, 4)
                }), 0.2f, 1, 1),
                new BossLoot("punch_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PUNCH, 2)
                }), 0.2f, 1, 1),
                new BossLoot("sharpness_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 5)
                }), 0.1f, 1, 1),
                new BossLoot("piercing_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PIERCING, 4)
                }), 0.1f, 1, 1),
                new BossLoot("power_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.POWER, 5)
                }), 0.1f, 1, 1),
                new BossLoot("quick_charge_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.QUICK_CHARGE, 3)
                }), 0.1f, 1, 1)
        });

        // SAMURICE
        this.addBossDrops("samurice", new BossLoot[] {
                new BossLoot("ancient_debris", new ItemStack(Items.ANCIENT_DEBRIS), 1, 1, 4),
                new BossLoot("iron", new ItemStack(Items.IRON_INGOT), 1, 4, 12),
                new BossLoot("emerald", new ItemStack(Items.EMERALD), 0.8f, 6, 20),
                new BossLoot("diamond", new ItemStack(Items.DIAMOND), 0.7f, 4, 32),
                new BossLoot("gold", new ItemStack(Items.GOLD_INGOT), 0.6f, 4, 16),
                new BossLoot("mending", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MENDING, 1)
                }), 0.6f, 1, 1),
                new BossLoot("packed_ice", new ItemStack(Items.PACKED_ICE), 0.5f, 12, 32),
                new BossLoot("blue_ice", new ItemStack(Items.BLUE_ICE), 0.5f, 12, 32),
                new BossLoot("ice", new ItemStack(Items.ICE), 0.5f, 8, 24),
                new BossLoot("diamond_pickaxe", new ItemStack(Items.DIAMOND_PICKAXE), 0.5f, 1, 1),
                new BossLoot("unbreaking_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 2)
                }), 0.5f, 1, 1),
                new BossLoot("sharpness_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 3)
                }), 0.5f, 1, 1),
                new BossLoot("infinity", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.INFINITY, 1)
                }), 0.5f, 1, 1),
                new BossLoot("netherite_pickaxe", new ItemStack(Items.NETHERITE_PICKAXE), 0.4f, 1, 1),
                new BossLoot("unbreaking_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.UNBREAKING, 3)
                }), 0.4f, 1, 1),
                new BossLoot("piercing_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PIERCING, 3)
                }), 0.4f, 1, 1),
                new BossLoot("aqua_affinity", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.AQUA_AFFINITY, 1)
                }), 0.4f, 1, 1),
                new BossLoot("respiration_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.RESPIRATION, 2)
                }), 0.4f, 1, 1),
                new BossLoot("depth_strider_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.DEPTH_STRIDER, 2)
                }), 0.4f, 1, 1),
                new BossLoot("feather_falling_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FEATHER_FALLING, 2)
                }), 0.4f, 1, 1),
                new BossLoot("fortune_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FORTUNE, 2)
                }), 0.4f, 1, 1),
                new BossLoot("swift_sneak_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SWIFT_SNEAK, 2)
                }), 0.4f, 1, 1),
                new BossLoot("quick_charge_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.QUICK_CHARGE, 2)
                }), 0.4f, 1, 1),
                new BossLoot("multishot", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.MULTISHOT, 1)
                }), 0.4f, 1, 1),
                new BossLoot("fortune_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FORTUNE, 3)
                }), 0.3f, 1, 1),
                new BossLoot("respiration_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.RESPIRATION, 3)
                }), 0.3f, 1, 1),
                new BossLoot("quick_charge_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.QUICK_CHARGE, 3)
                }), 0.3f, 1, 1),
                new BossLoot("piercing_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PIERCING, 4)
                }), 0.3f, 1, 1),
                new BossLoot("depth_strider_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.DEPTH_STRIDER, 3)
                }), 0.3f, 1, 1),
                new BossLoot("feather_falling_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FEATHER_FALLING, 3)
                }), 0.3f, 1, 1),
                new BossLoot("frost_walker_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FROST_WALKER, 2)
                }), 0.3f, 1, 1),
                new BossLoot("protection_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 3)
                }), 0.3f, 1, 1),
                new BossLoot("efficiency_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.EFFICIENCY, 4)
                }), 0.3f, 1, 1),
                new BossLoot("looting_2", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LOOTING, 2)
                }), 0.3f, 1, 1),
                new BossLoot("sharpness_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 4)
                }), 0.3f, 1, 1),
                new BossLoot("feather_falling_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.FEATHER_FALLING, 4)
                }), 0.2f, 1, 1),
                new BossLoot("protection_4", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.PROTECTION, 4)
                }), 0.2f, 1, 1),
                new BossLoot("swift_sneak_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SWIFT_SNEAK, 3)
                }), 0.2f, 1, 1),
                new BossLoot("efficiency_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.EFFICIENCY, 5)
                }), 0.2f, 1, 1),
                new BossLoot("looting_3", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.LOOTING, 3)
                }), 0.2f, 1, 1),
                new BossLoot("sharpness_5", this.createEnchantedBook(registries, new CustomEnchantment[] {
                        new CustomEnchantment(Enchantments.SHARPNESS, 5)
                }), 0.2f, 1, 1),
        });
    }



    private record BossLoot(String name, ItemStack item, float probability, int min, int max) {}

    private record CustomEnchantment(ResourceKey<Enchantment> name, int level) {}
}

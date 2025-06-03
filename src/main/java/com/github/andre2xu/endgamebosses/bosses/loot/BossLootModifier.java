package com.github.andre2xu.endgamebosses.bosses.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class BossLootModifier extends LootModifier {
    private final Item custom_loot;
    private final int min_amount;
    private final int max_amount;

    public static final MapCodec<BossLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> LootModifier.codecStart(inst)
            .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("custom_loot").forGetter(modifier -> modifier.custom_loot))
            .and(Codec.INT.fieldOf("min").forGetter(modifier -> modifier.min_amount))
            .and(Codec.INT.fieldOf("max").forGetter(modifier -> modifier.max_amount))
            .apply(inst, BossLootModifier::new)); // this codec specifies how to read the loot item's data from the loot table JSON file



    public BossLootModifier(LootItemCondition[] conditionsIn, Item customLoot, int minAmount, int maxAmount) {
        super(conditionsIn);

        this.custom_loot = customLoot;
        this.min_amount = minAmount;
        this.max_amount = maxAmount;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // check if a condition is NOT met and if so do not drop custom loot
        for (LootItemCondition condition : this.conditions) {
            if(!condition.test(context)) {
                return generatedLoot;
            }
        }

        // calculate the amount to drop
        int amount = new Random().nextInt(this.min_amount, this.max_amount + 1);

        // add custom loot to drops
        generatedLoot.add(new ItemStack(this.custom_loot, amount));

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

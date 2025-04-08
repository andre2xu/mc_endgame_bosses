package com.github.andre2xu.endgamebosses.bosses;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BossRegistry {
    public static DeferredRegister<EntityType<?>> BOSS_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EndgameBosses.MODID);

    // TYPES
    public static RegistryObject<EntityType<MechalodonEntity>> MECHALODON = BOSS_TYPES.register("mechalodon", () -> EntityType.Builder.of(MechalodonEntity::new, MobCategory.MONSTER).build("mechalodon"));



    // this is called in the 'EndgameBosses' class constructor
    public static void register(IEventBus eventBus) {
        BOSS_TYPES.register(eventBus);
    }
}

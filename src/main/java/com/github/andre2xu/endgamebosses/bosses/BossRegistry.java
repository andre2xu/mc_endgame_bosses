package com.github.andre2xu.endgamebosses.bosses;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BossRegistry {
    public static DeferredRegister<EntityType<?>> BOSS_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EndgameBosses.MODID);



    // this is called in the 'EndgameBosses' class constructor
    public static void register(IEventBus eventBus) {
        BOSS_TYPES.register(eventBus);
    }
}

package com.github.andre2xu.endgamebosses.bosses;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.mama.egg_sac.MamaEggSacEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MiscEntityRegistry {
    // NOTE: this registry is only for entities that are neither a boss nor a projectile
    
    public static DeferredRegister<EntityType<?>> MISC_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EndgameBosses.MODID);
    
    // TYPES
    public static RegistryObject<EntityType<MamaEggSacEntity>> MAMA_EGG_SAC = MISC_ENTITY_TYPES.register("mama_egg_sac", () -> EntityType.Builder.of(MamaEggSacEntity::new, MobCategory.MISC).build("mama_egg_sac"));
    
    
    
    // this is called in the 'EndgameBosses' class constructor
    public static void register(IEventBus eventBus) {
        MISC_ENTITY_TYPES.register(eventBus);
    }
}

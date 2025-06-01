package com.github.andre2xu.endgamebosses.bosses;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.mama.egg_sac.MamaEggSacEntity;
import com.github.andre2xu.endgamebosses.bosses.mama.spiderling.SpiderlingEntity;
import com.github.andre2xu.endgamebosses.bosses.samurice.clone.SamuriceCloneEntity;
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
    public static RegistryObject<EntityType<MamaEggSacEntity>> MAMA_EGG_SAC = MISC_ENTITY_TYPES.register("mama_egg_sac", () -> EntityType.Builder.of(MamaEggSacEntity::new, MobCategory.MISC).sized(7f, 10f).build("mama_egg_sac"));

    public static RegistryObject<EntityType<SpiderlingEntity>> SPIDERLING = MISC_ENTITY_TYPES.register("spiderling", () -> EntityType.Builder.of(SpiderlingEntity::new, MobCategory.MISC).sized(3f, 1.3f).build("spiderling"));

    public static RegistryObject<EntityType<SamuriceCloneEntity>> SAMURICE_CLONE = MISC_ENTITY_TYPES.register("samurice_clone", () -> EntityType.Builder.of(SamuriceCloneEntity::new, MobCategory.MISC).sized(0.5f, 2.5f).build("samurice_clone"));
    
    
    
    // this is called in the 'EndgameBosses' class constructor
    public static void register(IEventBus eventBus) {
        MISC_ENTITY_TYPES.register(eventBus);
    }
}

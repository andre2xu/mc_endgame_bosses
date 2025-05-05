package com.github.andre2xu.endgamebosses.bosses;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.missile.MechalodonMissileEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.icicle.TragonIcicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ProjectilesRegistry {
    public static DeferredRegister<EntityType<?>> PROJECTILE_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EndgameBosses.MODID);

    // TYPES
    public static RegistryObject<EntityType<MechalodonMissileEntity>> MECHALODON_MISSILE = PROJECTILE_TYPES.register("mechalodon_missile", () -> EntityType.Builder.of(MechalodonMissileEntity::new, MobCategory.MISC).sized(1f, 0.5f).build("mechalodon_missile"));

    public static RegistryObject<EntityType<TragonIcicleEntity>> TRAGON_ICICLE = PROJECTILE_TYPES.register("tragon_icicle", () -> EntityType.Builder.of(TragonIcicleEntity::new, MobCategory.MISC).sized(1.5f, 7f).build("tragon_icicle"));



    // this is called in the 'EndgameBosses' class constructor
    public static void register(IEventBus eventBus) {
        PROJECTILE_TYPES.register(eventBus);
    }
}

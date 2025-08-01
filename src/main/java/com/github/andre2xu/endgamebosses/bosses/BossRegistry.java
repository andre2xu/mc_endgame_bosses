package com.github.andre2xu.endgamebosses.bosses;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import com.github.andre2xu.endgamebosses.bosses.mechalodon.MechalodonEntity;
import com.github.andre2xu.endgamebosses.bosses.samurice.SamuriceEntity;
import com.github.andre2xu.endgamebosses.bosses.tragon.TragonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BossRegistry {
    public static DeferredRegister<EntityType<?>> BOSS_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EndgameBosses.MODID);

    // TYPES
    public static RegistryObject<EntityType<MechalodonEntity>> MECHALODON = BOSS_TYPES.register("mechalodon", () -> EntityType.Builder.of(MechalodonEntity::new, MobCategory.MONSTER).sized(10f, 4.5f).build("mechalodon"));

    public static RegistryObject<EntityType<TragonEntity>> TRAGON = BOSS_TYPES.register("tragon", () -> EntityType.Builder.of(TragonEntity::new, MobCategory.MONSTER).sized(13f, 11f).build("tragon"));

    public static RegistryObject<EntityType<MamaEntity>> MAMA = BOSS_TYPES.register("mama", () -> EntityType.Builder.of(MamaEntity::new, MobCategory.MONSTER).sized(20f, 6f).build("mama"));

    public static RegistryObject<EntityType<SamuriceEntity>> SAMURICE = BOSS_TYPES.register("samurice", () -> EntityType.Builder.of(SamuriceEntity::new, MobCategory.MONSTER).sized(0.5f, 2.5f).build("samurice"));



    // this is called in the 'EndgameBosses' class constructor
    public static void register(IEventBus eventBus) {
        BOSS_TYPES.register(eventBus);
    }
}

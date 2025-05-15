package com.github.andre2xu.endgamebosses.bosses.mama.egg_sac;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MamaEggSacModel extends GeoModel<MamaEggSacEntity> {
    @Override
    public ResourceLocation getModelResource(MamaEggSacEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/mama/mama_egg_sac.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MamaEggSacEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/mama/mama_egg_sac.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MamaEggSacEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/mama/mama_egg_sac.animation.json");
    }
}

package com.github.andre2xu.endgamebosses.bosses.mama.spiderling;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.mama.MamaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SpiderlingModel extends GeoModel<MamaEntity> {
    @Override
    public ResourceLocation getModelResource(MamaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/mama/mama.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MamaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/mama/mama.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MamaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/mama/mama.animation.json");
    }
}

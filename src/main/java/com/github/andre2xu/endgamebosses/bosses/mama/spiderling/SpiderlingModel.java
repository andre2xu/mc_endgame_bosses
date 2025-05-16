package com.github.andre2xu.endgamebosses.bosses.mama.spiderling;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SpiderlingModel extends GeoModel<SpiderlingEntity> {
    @Override
    public ResourceLocation getModelResource(SpiderlingEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/mama/spiderling.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SpiderlingEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/mama/mama.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SpiderlingEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/mama/mama.animation.json");
    }
}

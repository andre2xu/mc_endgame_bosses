package com.github.andre2xu.endgamebosses.bosses.tragon;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TragonModel extends GeoModel<TragonEntity> {
    @Override
    public ResourceLocation getModelResource(TragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/tragon/tragon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/tragon/tragon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/tragon/tragon.animation.json");
    }
}

package com.github.andre2xu.endgamebosses.bosses.tragon.icicle;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TragonIcicleModel extends GeoModel<TragonIcicleEntity> {
    @Override
    public ResourceLocation getModelResource(TragonIcicleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/tragon/tragon_icicle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TragonIcicleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/tragon/tragon_icicle.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TragonIcicleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/tragon/tragon_icicle.animation.json");
    }
}

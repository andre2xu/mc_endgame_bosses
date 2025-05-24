package com.github.andre2xu.endgamebosses.bosses.samurice;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SamuriceModel extends GeoModel<SamuriceEntity> {
    @Override
    public ResourceLocation getModelResource(SamuriceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/samurice/samurice.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SamuriceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/samurice/samurice.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SamuriceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/samurice/samurice.animation.json");
    }
}

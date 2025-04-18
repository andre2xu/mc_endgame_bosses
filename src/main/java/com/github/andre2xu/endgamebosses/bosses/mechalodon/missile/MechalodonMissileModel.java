package com.github.andre2xu.endgamebosses.bosses.mechalodon.missile;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MechalodonMissileModel extends GeoModel<MechalodonMissileEntity> {
    @Override
    public ResourceLocation getModelResource(MechalodonMissileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/mechalodon/mechalodon_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MechalodonMissileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/mechalodon/mechalodon_missile.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MechalodonMissileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/mechalodon/mechalodon_missile.animation.json");
    }
}

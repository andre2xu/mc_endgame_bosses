package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Optional;

public class MechalodonModel extends GeoModel<MechalodonEntity> {
    @Override
    public ResourceLocation getModelResource(MechalodonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "geo/entity/mechalodon/mechalodon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MechalodonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "textures/entity/mechalodon/mechalodon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MechalodonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "animations/entity/mechalodon/mechalodon.animation.json");
    }

    @Override
    public void setCustomAnimations(MechalodonEntity animatable, long instanceId, AnimationState<MechalodonEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Optional<GeoBone> body_bone = getBone("body");
        body_bone.ifPresent(body -> body.setRotX(animatable.getBodyPitch()));
    }
}

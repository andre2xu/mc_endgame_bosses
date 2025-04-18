package com.github.andre2xu.endgamebosses.bosses.mechalodon.missile;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Optional;

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

    @Override
    public void setCustomAnimations(MechalodonMissileEntity animatable, long instanceId, AnimationState<MechalodonMissileEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Optional<GeoBone> body_bone = getBone("body");
        body_bone.ifPresent(body -> body.setRotX(animatable.getBodyPitch()));
    }
}

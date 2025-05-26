package com.github.andre2xu.endgamebosses.bosses.samurice;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Objects;

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

    @Override
    public void setCustomAnimations(SamuriceEntity animatable, long instanceId, AnimationState<SamuriceEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        boolean rotate_head = true;

        // check what animation is running and decide whether to allow the rotation of the head or not
        AnimationProcessor.QueuedAnimation current_animation = animationState.getController().getCurrentAnimation();

        if (current_animation != null) {
            String running_animation_name = current_animation.animation().name();

            if (Objects.equals(running_animation_name, "animation.samurice.swim")) {
                rotate_head = false;
            }
        }

        if (rotate_head) {
            getBone("head").ifPresent(head -> head.setRotX(animatable.getHeadPitch()));
        }
    }
}

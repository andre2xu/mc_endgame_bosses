package com.github.andre2xu.endgamebosses.bosses.mechalodon;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.networking.MainChannel;
import com.github.andre2xu.endgamebosses.networking.shared.packets.ModelBonePositionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Objects;
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

        boolean rotate_body = true;

        // check what animation is running and decide whether to allow the rotation of the body or not
        AnimationProcessor.QueuedAnimation current_animation = animationState.getController().getCurrentAnimation();

        if (current_animation != null) {
            String running_animation_name = current_animation.animation().name();

            if (Objects.equals(running_animation_name, "animation.mechalodon.face_up") || Objects.equals(running_animation_name, "animation.mechalodon.face_down")) {
                rotate_body = false;
            }
            else if (Objects.equals(running_animation_name, "animation.mechalodon.face_up_reverse") || Objects.equals(running_animation_name, "animation.mechalodon.face_down_reverse")) {
                rotate_body = animationState.getController().hasAnimationFinished();
            }
        }

        // this is for 'LookControl.setLookAt' to work
        if (rotate_body) {
            Optional<GeoBone> body_bone = getBone("body");
            body_bone.ifPresent(body -> body.setRotX(animatable.getBodyPitch()));
        }

        // get the world positions of the Mechalodon's particle-producing bones and send them to the server side
        String[] model_bone_names = {
                "cannon",
                "side_thruster1",
                "side_thruster2",
                "back_thruster"
        };

        for (String bone_name : model_bone_names) {
            Optional<GeoBone> bone = getBone(bone_name);

            bone.ifPresent(model_bone -> {
                Vector3d world_pos = model_bone.getWorldPosition();

                MainChannel.sendToServer(new ModelBonePositionsPacket(
                        instanceId,
                        model_bone.getName(),
                        new Vec3(world_pos.x, world_pos.y, world_pos.z))
                );
            });
        }
    }
}

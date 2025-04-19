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
                // NOTE: the world positions aren't precise even with the corrections but they should be close to the bone. When spawning particles, the position will have to be further adjusted
                
                Vector3d world_pos = model_bone.getWorldPosition(); // for some reason this is only accurate for the top-most bone, not for child bones
                Vector3d local_pos = model_bone.getLocalPosition(); // get position of bone within its parent bone
                Vector3d corrected_pos = world_pos.add(local_pos); // correct world position

                if (Objects.equals(model_bone.getName(), "back_thruster")) {
                    Vector3d parent_local_pos = model_bone.getParent().getLocalPosition(); // extra correction
                    corrected_pos = corrected_pos.add(parent_local_pos.x, 0, parent_local_pos.z);
                }

                MainChannel.sendToServer(new ModelBonePositionsPacket(
                        instanceId,
                        model_bone.getName(),
                        new Vec3(corrected_pos.x, corrected_pos.y, corrected_pos.z))
                );
            });
        }
    }
}

package com.github.andre2xu.endgamebosses.bosses.tragon;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.FireHead;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.IceHead;
import com.github.andre2xu.endgamebosses.bosses.tragon.heads.LightningHead;
import com.github.andre2xu.endgamebosses.networking.MainChannel;
import com.github.andre2xu.endgamebosses.networking.shared.packets.ModelBonePositionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.Objects;

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

    @Override
    public void setCustomAnimations(TragonEntity animatable, long instanceId, AnimationState<TragonEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // set pitch rotation of heads
        this.setPitchOfHeads(animatable.getHeadPitch());

        // set position of neck hitboxes
        this.updateNeckHitbox(animatable, instanceId, "fire_head_neck", "fh_skull");
        this.updateNeckHitbox(animatable, instanceId, "lightning_head_neck", "lh_skull");
        this.updateNeckHitbox(animatable, instanceId, "ice_head_neck", "ih_skull");

        // render the dead heads as headless
        this.renderDeadHeadsAsHeadless(animatable);

        // send bone positions to entity class
        String[] bone_names = {
                "fh_skull_mouth_lower",
                "lh_skull_mouth_lower",
                "ih_skull_mouth_lower"
        };

        this.updateBonePositions(animatable, instanceId, bone_names);
    }

    private void updateBonePositions(TragonEntity animatable, long instanceId, String[] boneNames) {
        for (String bone_name : boneNames) {
            getBone(bone_name).ifPresent(bone -> {
                Vector3d bone_world_pos = bone.getWorldPosition().add(bone.getLocalPosition());

                // make some corrections to the world positions since they aren't accurate. The corrections are only approximations
                if (Objects.equals(bone_name, "fh_skull_mouth_lower") || Objects.equals(bone_name, "lh_skull_mouth_lower") || Objects.equals(bone_name, "ih_skull_mouth_lower")) {
                    Vector3d skull_world_pos = bone.getParent().getLocalPosition();
                    Vector3d neck_upper_world_pos = bone.getParent().getParent().getLocalPosition();
                    Vector3d neck_middle_world_pos = bone.getParent().getParent().getParent().getLocalPosition();
                    Vector3d neck_lower_world_pos = bone.getParent().getParent().getParent().getLocalPosition();

                    if (Objects.equals(bone_name, "ih_skull_mouth_lower")) {
                        bone_world_pos = bone_world_pos.add(skull_world_pos.add(neck_upper_world_pos.add(neck_middle_world_pos))); // this correction is better for the ice head
                    }
                    else {
                        bone_world_pos = bone_world_pos.add(skull_world_pos.add(neck_upper_world_pos.add(neck_middle_world_pos.add(neck_lower_world_pos))));
                    }
                }

                Vec3 bone_pos = new Vec3(bone_world_pos.x, bone_world_pos.y, bone_world_pos.z);

                // update bone position on server side
                MainChannel.sendToServer(new ModelBonePositionsPacket(
                        instanceId,
                        bone_name,
                        bone_pos
                ));

                // update bone position on client side
                animatable.updateBonePosition(bone_name, bone_pos);
            });
        }
    }

    private void updateNeckHitbox(TragonEntity animatable, long instanceId, String hitboxEntityName, String skullName) {
        // NOTE: world positions are wrong so the skull bone, rather than a neck bone, is used to approximate the hitbox position

        getBone(skullName).ifPresent(bone -> {
            Vector3d bone_world_pos = bone.getWorldPosition().add(bone.getLocalPosition());
            Vec3 bone_pos = new Vec3(bone_world_pos.x, bone_world_pos.y, bone_world_pos.z);

            // update hitbox position on server side
            MainChannel.sendToServer(new ModelBonePositionsPacket(
                    instanceId,
                    hitboxEntityName,
                    bone_pos
            ));

            // update hitbox position on client side
            animatable.updateHitboxPosition(hitboxEntityName, bone_pos);
        });
    }

    private void setPitchOfHeads(float new_pitch) {
        ArrayList<String> bones_to_rotate = new ArrayList<>();

        if (new_pitch < 0) {
            bones_to_rotate.add("fh_neck_lower");
            bones_to_rotate.add("fh_skull");
            bones_to_rotate.add("lh_skull");
            bones_to_rotate.add("ih_neck_middle");
            bones_to_rotate.add("ih_skull");

            if (new_pitch < -0.41f) {
                new_pitch = -0.41f;
            }
        }
        else if (new_pitch > 0) {
            bones_to_rotate.add("fh_neck_lower");
            bones_to_rotate.add("lh_skull");
            bones_to_rotate.add("ih_neck_lower");

            if (new_pitch > 0.55f) {
                new_pitch = 0.55f;
            }
        }

        // rotate bones to make the Tragon's heads face a target or block
        if (!bones_to_rotate.isEmpty()) {
            for (String bone_name : bones_to_rotate) {
                final float PITCH = new_pitch;

                getBone(bone_name).ifPresent(bone -> bone.setRotX(PITCH));
            }
        }
    }

    private void renderDeadHeadsAsHeadless(TragonEntity animatable) {
        boolean fire_head_is_alive = animatable.getHeadAliveFlag(FireHead.class);
        boolean lightning_head_is_alive = animatable.getHeadAliveFlag(LightningHead.class);
        boolean ice_head_is_alive = animatable.getHeadAliveFlag(IceHead.class);

        // hide bone if dead, don't hide bone if alive
        getBone("fh_neck_middle").ifPresent(bone -> bone.setHidden(!fire_head_is_alive));
        getBone("lh_neck_middle").ifPresent(bone -> bone.setHidden(!lightning_head_is_alive));
        getBone("ih_neck_middle").ifPresent(bone -> bone.setHidden(!ice_head_is_alive));
    }
}

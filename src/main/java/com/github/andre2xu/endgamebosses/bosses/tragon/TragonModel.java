package com.github.andre2xu.endgamebosses.bosses.tragon;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.networking.MainChannel;
import com.github.andre2xu.endgamebosses.networking.shared.packets.ModelBonePositionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimationState;
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

    @Override
    public void setCustomAnimations(TragonEntity animatable, long instanceId, AnimationState<TragonEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        updateNeckHitbox(animatable, instanceId, "fire_head_neck", "fh_skull");
        updateNeckHitbox(animatable, instanceId, "lightning_head_neck", "lh_skull");
        updateNeckHitbox(animatable, instanceId, "ice_head_neck", "ih_skull");
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
}

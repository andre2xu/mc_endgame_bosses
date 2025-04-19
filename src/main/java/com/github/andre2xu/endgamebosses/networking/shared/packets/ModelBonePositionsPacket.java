package com.github.andre2xu.endgamebosses.networking.shared.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ModelBonePositionsPacket {
    private final long entity_id;
    private final String bone_name;
    private final Vec3 bone_pos;

    public ModelBonePositionsPacket(long entityId, String boneName, Vec3 bonePos) {
        this.entity_id = entityId;
        this.bone_name = boneName;
        this.bone_pos = bonePos;
    }

    public ModelBonePositionsPacket(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        this(registryFriendlyByteBuf.readLong(), registryFriendlyByteBuf.readUtf(), registryFriendlyByteBuf.readVec3());
    }

    public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeLong(this.entity_id);
        registryFriendlyByteBuf.writeUtf(this.bone_name);
        registryFriendlyByteBuf.writeVec3(this.bone_pos);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            System.out.println("HANDLING BONE POSITIONS");
        });

        context.setPacketHandled(true);
    }
}

package com.github.andre2xu.endgamebosses.networking;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import com.github.andre2xu.endgamebosses.networking.shared.packets.ModelBonePositionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class MainChannel {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(EndgameBosses.MODID, "main"))
            .serverAcceptedVersions((status, version) -> true)
            .clientAcceptedVersions((status, version) -> true)
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void registerPacketsToProcess() {
        // model bone positions (client to server)
        CHANNEL.messageBuilder(ModelBonePositionsPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ModelBonePositionsPacket::encode)
                .decoder(ModelBonePositionsPacket::new)
                .consumerMainThread(ModelBonePositionsPacket::handle)
                .add();
    }

    public static void sendToServer(Object packetClassInstance) {
        CHANNEL.send(packetClassInstance, PacketDistributor.SERVER.noArg());
    }
}

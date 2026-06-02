package com.example.nativeanticheat.client;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 服务端 -> 客户端：握手挑战
 */
public record ChallengePayload(long nonce) implements CustomPayload {

    public static final CustomPayload.Id<ChallengePayload> ID =
            new CustomPayload.Id<>(Identifier.of("native-anticheat", "challenge"));

    public static final PacketCodec<PacketByteBuf, ChallengePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_LONG, ChallengePayload::nonce,
                    ChallengePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
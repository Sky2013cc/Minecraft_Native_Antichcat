package com.example.nativeanticheat;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record ModListPayload(
        List<String> modIds,
        List<String> fileHashes,
        List<String> suspiciousHits,
        long clientNonce,
        String integritySignature
) implements CustomPayload {

    public static final CustomPayload.Id<ModListPayload> ID =
            new CustomPayload.Id<>(Identifier.of("native-anticheat", "mod_list"));

    public static final PacketCodec<PacketByteBuf, ModListPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING.collect(PacketCodecs.toList()), ModListPayload::modIds,
                    PacketCodecs.STRING.collect(PacketCodecs.toList()), ModListPayload::fileHashes,
                    PacketCodecs.STRING.collect(PacketCodecs.toList()), ModListPayload::suspiciousHits,
                    PacketCodecs.VAR_LONG, ModListPayload::clientNonce,
                    PacketCodecs.STRING, ModListPayload::integritySignature,
                    ModListPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
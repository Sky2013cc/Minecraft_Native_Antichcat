package com.example.nativeanticheat.client;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * 客户端 -> 服务端：mod 列表 + 环境指纹 + 完整性签名
 */
public record ModListPayload(
        List<String> modIds,
        List<String> fileHashes,    // 各 mod jar 的哈希
        List<String> suspiciousHits,// 环境扫描可疑项
        long clientNonce,           // 服务端下发的挑战值回显
        String integritySignature   // 对上述内容的 HMAC 签名
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
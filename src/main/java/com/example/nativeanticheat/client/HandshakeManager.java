package com.example.nativeanticheat.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

/**
 * 处理与服务端的握手：
 *  服务端下发 nonce -> 客户端收集信息 -> 签名 -> 回传
 */
public class HandshakeManager {

    public static void respondToChallenge(long nonce) {
        // 1. 收集 mod 信息
        ModCollector.ModInfo info = ModCollector.collect();

        // 2. 环境扫描
        List<String> suspicious = EnvironmentScanner.scan();

        // 3. 自检
        if (IntegrityChecker.isSelfTampered()) {
            suspicious.add("self:tampered");
        }

        // 4. 基于服务端 nonce 签名（防重放、防伪造）
        String signature = IntegrityChecker.sign(
                info.modIds(),
                info.fileHashes(),
                suspicious,
                nonce
        );

        // 5. 构造并发送载荷
        ModListPayload payload = new ModListPayload(
                info.modIds(),
                info.fileHashes(),
                suspicious,
                nonce,
                signature
        );

        if (ClientPlayNetworking.canSend(ModListPayload.ID)) {
            ClientPlayNetworking.send(payload);
            NativeAnticheatClient.LOGGER.info(
                    "Sent mod report: {} mods, {} suspicious hits",
                    info.modIds().size(), suspicious.size());
        } else {
            NativeAnticheatClient.LOGGER.warn("Server does not accept mod_list payload");
        }
    }
}
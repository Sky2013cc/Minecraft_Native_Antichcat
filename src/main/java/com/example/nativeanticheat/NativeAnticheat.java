package com.example.nativeanticheat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.List;

public class NativeAnticheat implements ModInitializer {

    public static final String MOD_ID = "native-anticheat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing native-anticheat (server)...");

        // 1. 加载白名单
        ModWhitelist.load();

        // 2. 注册网络载荷
        //    【改动】新增 S2C challenge 通道注册
        PayloadTypeRegistry.playC2S().register(ModListPayload.ID, ModListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChallengePayload.ID, ChallengePayload.CODEC);

        // 3. 注册接收处理器
        //    【改动】参数从 List<String> 变为完整的 ModListPayload
        ServerPlayNetworking.registerGlobalReceiver(ModListPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleModList(player, payload));
        });

        // 4. 玩家断开时清理 nonce
        //    【改动】新增 NonceStore 清理
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            NonceStore.clear(player.getUuid());
            LOGGER.debug("Player {} disconnected", player.getName().getString());
        });

        // 5. 玩家加入时下发挑战
        //    【改动】不再只是判断 canSend，而是主动下发 nonce
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;

            if (ServerPlayNetworking.canSend(player, ChallengePayload.ID)) {
                long nonce = RANDOM.nextLong();
                NonceStore.put(player.getUuid(), nonce);
                ServerPlayNetworking.send(player, new ChallengePayload(nonce));
                LOGGER.info("Sent challenge to {}", player.getName().getString());
            } else {
                // 客户端未安装本反作弊
                LOGGER.warn("[SUSPECT] Player {} has no native-anticheat client",
                        player.getName().getString());
                SuspectManager.markSuspect(player, List.of("__no_anticheat_client__"));
            }
        });

        LOGGER.info("native-anticheat (server) initialized.");
    }

    /**
     * 【改动】整个方法重写：增加 nonce 校验 + 签名校验
     */
    private void handleModList(ServerPlayerEntity player, ModListPayload payload) {
        // 5.1 校验 nonce（防重放、防伪造请求）
        boolean nonceOk = NonceStore.validateAndConsume(
                player.getUuid(), payload.clientNonce());
        if (!nonceOk) {
            LOGGER.warn("[SUSPECT] Player {} nonce mismatch/timeout",
                    player.getName().getString());
            SuspectManager.markSuspect(player, List.of("__nonce_mismatch__"));
            return;
        }

        // 5.2 校验签名（防篡改）
        String expectedSig = IntegrityChecker.sign(
                payload.modIds(),
                payload.fileHashes(),
                payload.suspiciousHits(),
                payload.clientNonce());
        if (!expectedSig.equals(payload.integritySignature())) {
            LOGGER.warn("[SUSPECT] Player {} bad signature",
                    player.getName().getString());
            SuspectManager.markSuspect(player, List.of("__bad_signature__"));
            return;
        }

        // 5.3 校验通过，检查客户端自报的可疑项
        if (!payload.suspiciousHits().isEmpty()) {
            LOGGER.warn("[SUSPECT] Player {} reported suspicious hits: {}",
                    player.getName().getString(), payload.suspiciousHits());
            SuspectManager.markSuspect(player, payload.suspiciousHits());
            return;
        }

        // 5.4 白名单比对
        List<String> illegal = ModWhitelist.findIllegal(payload.modIds());
        if (!illegal.isEmpty()) {
            SuspectManager.markSuspect(player, illegal);
        } else {
            SuspectManager.clear(player.getUuid());
            LOGGER.info("Player {} passed mod verification.",
                    player.getName().getString());
        }
    }
}
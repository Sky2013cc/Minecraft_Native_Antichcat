package com.example.nativeanticheat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeAnticheatClient implements ClientModInitializer {

    public static final String MOD_ID = "native-anticheat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing native-anticheat (client)...");

        // 注册载荷类型
        // C2S：上报
        PayloadTypeRegistry.playC2S().register(ModListPayload.ID, ModListPayload.CODEC);
        // S2C：挑战
        PayloadTypeRegistry.playS2C().register(ChallengePayload.ID, ChallengePayload.CODEC);

        // 接收服务端挑战
        ClientPlayNetworking.registerGlobalReceiver(ChallengePayload.ID, (payload, context) -> {
            long nonce = payload.nonce();
            // 在客户端线程执行收集与发送
            context.client().execute(() -> {
                LOGGER.info("Received challenge, preparing report...");
                HandshakeManager.respondToChallenge(nonce);
            });
        });

        LOGGER.info("native-anticheat (client) initialized.");
    }
}
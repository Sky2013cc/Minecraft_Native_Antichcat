package com.example.nativeanticheat;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理被标记为“疑似作弊”的玩家。
 * 该标签对玩家不可见，仅服务端记录。
 */
public class SuspectManager {

    public record SuspectInfo(List<String> illegalMods, long timestamp) {}

    private static final Map<UUID, SuspectInfo> suspects = new ConcurrentHashMap<>();

    public static void markSuspect(ServerPlayerEntity player, List<String> illegalMods) {
        suspects.put(player.getUuid(),
                new SuspectInfo(illegalMods, System.currentTimeMillis()));

        // 仅在服务端日志记录，玩家本身不会收到任何提示
        NativeAnticheat.LOGGER.warn(
                "[SUSPECT] Player {} ({}) has illegal mods: {}",
                player.getName().getString(),
                player.getUuidAsString(),
                illegalMods
        );
    }

    public static boolean isSuspect(UUID uuid) {
        return suspects.containsKey(uuid);
    }

    public static SuspectInfo getInfo(UUID uuid) {
        return suspects.get(uuid);
    }

    public static void clear(UUID uuid) {
        suspects.remove(uuid);
    }

    public static Map<UUID, SuspectInfo> getAllSuspects() {
        return Map.copyOf(suspects);
    }
}
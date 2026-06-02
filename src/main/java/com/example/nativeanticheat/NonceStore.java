package com.example.nativeanticheat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储每个玩家加入时下发的 nonce，用于校验回传。
 */
public class NonceStore {

    private record Entry(long nonce, long issuedAt) {}

    private static final Map<UUID, Entry> store = new ConcurrentHashMap<>();

    // nonce 有效期（毫秒），超过则视为超时
    private static final long TIMEOUT_MS = 30_000L;

    public static void put(UUID uuid, long nonce) {
        store.put(uuid, new Entry(nonce, System.currentTimeMillis()));
    }

    /**
     * 取出并校验 nonce 是否匹配且未超时。
     * 校验后立即移除（一次性使用，防重放）。
     */
    public static boolean validateAndConsume(UUID uuid, long nonce) {
        Entry entry = store.remove(uuid);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() - entry.issuedAt() > TIMEOUT_MS) {
            return false; // 超时
        }
        return entry.nonce() == nonce;
    }

    public static void clear(UUID uuid) {
        store.remove(uuid);
    }
}
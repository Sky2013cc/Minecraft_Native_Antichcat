package com.example.nativeanticheat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 读取/管理白名单 mod 列表。
 * 配置文件位于 config/native-anticheat/whitelist.json
 */
public class ModWhitelist {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> whitelist = new HashSet<>();
    private static Path configPath;

    public static void load() {
        Path configDir = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("native-anticheat");
        configPath = configDir.resolve("whitelist.json");

        try {
            Files.createDirectories(configDir);

            if (!Files.exists(configPath)) {
                // 默认白名单：常见的官方/无害 mod
                List<String> defaults = List.of(
                        "minecraft",
                        "fabricloader",
                        "fabric-api",
                        "java",
                        "native-anticheat"
                );
                Files.writeString(configPath, GSON.toJson(defaults));
                whitelist.addAll(defaults);
                NativeAnticheat.LOGGER.info("Created default whitelist at {}", configPath);
                return;
            }

            String json = Files.readString(configPath);
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> loaded = GSON.fromJson(json, listType);
            whitelist.clear();
            if (loaded != null) {
                whitelist.addAll(loaded);
            }
            NativeAnticheat.LOGGER.info("Loaded {} whitelisted mods", whitelist.size());

        } catch (IOException e) {
            NativeAnticheat.LOGGER.error("Failed to load whitelist", e);
        }
    }

    public static boolean isWhitelisted(String modId) {
        return whitelist.contains(modId);
    }

    /**
     * 返回不在白名单中的 mod 列表
     */
    public static List<String> findIllegal(List<String> clientMods) {
        return clientMods.stream()
                .filter(id -> !isWhitelisted(id))
                .toList();
    }

    public static Set<String> getWhitelist() {
        return new HashSet<>(whitelist);
    }
}
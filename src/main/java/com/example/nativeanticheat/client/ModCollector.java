package com.example.nativeanticheat.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 收集已加载的 mod 及其文件哈希。
 */
public class ModCollector {

    public record ModInfo(List<String> modIds, List<String> fileHashes) {}

    public static ModInfo collect() {
        List<String> ids = new ArrayList<>();
        List<String> hashes = new ArrayList<>();

        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();

        for (ModContainer mod : mods) {
            String id = mod.getMetadata().getId();
            ids.add(id);

            // 尝试计算 mod 主文件的哈希
            String hash = computeModHash(mod);
            hashes.add(id + ":" + hash);
        }

        return new ModInfo(ids, hashes);
    }

    private static String computeModHash(ModContainer mod) {
        try {
            List<Path> roots = mod.getRootPaths();
            if (roots.isEmpty()) {
                return "no-root";
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 对 fabric.mod.json 做哈希（稳定且能反映 mod 身份）
            Path root = roots.get(0);
            Path metaFile = root.resolve("fabric.mod.json");

            if (Files.exists(metaFile)) {
                try (InputStream in = Files.newInputStream(metaFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        digest.update(buf, 0, read);
                    }
                }
                return HexFormat.of().formatHex(digest.digest());
            }
            return "no-meta";

        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 获取源 jar 路径（用于完整性校验）
     */
    public static Optional<Path> getOwnJarPath() {
        return FabricLoader.getInstance()
                .getModContainer("native-anticheat")
                .flatMap(c -> c.getRootPaths().stream().findFirst());
    }
}
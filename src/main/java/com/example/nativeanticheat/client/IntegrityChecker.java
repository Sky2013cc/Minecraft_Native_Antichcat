package com.example.nativeanticheat.client;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 负责：
 *  1. 对上报内容做 HMAC 签名（基于服务端下发的 nonce，防重放）
 *  2. 自身类的完整性校验（检测是否被运行时篡改）
 */
public class IntegrityChecker {

    // 注意：客户端密钥无法真正保密，这里只用于增加伪造成本。
    // 真正的信任应建立在服务端 nonce + 行为分析上。
    private static final String SHARED_SECRET = "native-anticheat-v1-shared-key";

    /**
     * 对上报数据生成 HMAC-SHA256 签名。
     * 包含 nonce 防止重放，包含全部内容防止篡改。
     */
    public static String sign(List<String> modIds,
                              List<String> hashes,
                              List<String> suspicious,
                              long nonce) {
        try {
            String data = String.join(",", modIds)
                    + "|" + String.join(",", hashes)
                    + "|" + String.join(",", suspicious)
                    + "|" + nonce;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);

        } catch (Exception e) {
            return "sign-error";
        }
    }

    /**
     * 检测关键类是否被 Mixin/Agent 重定义。
     * 通过比对方法字节码长度等启发式特征（仅作示例）。
     */
    public static boolean isSelfTampered() {
        try {
            // 检测是否存在常见的 Java Agent 注入痕迹
            String javaCommand = System.getProperty("sun.java.command", "");
            String inputArgs = java.lang.management.ManagementFactory
                    .getRuntimeMXBean().getInputArguments().toString();

            // 检测 -javaagent 注入（白名单外的 agent 视为可疑）
            if (inputArgs.contains("-javaagent")) {
                // 这里可结合白名单判断，简化为标记
                NativeAnticheatClient.LOGGER.warn("Detected javaagent in JVM args");
                return true;
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算任意字符串的 SHA-256（工具方法）
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "error";
        }
    }
}
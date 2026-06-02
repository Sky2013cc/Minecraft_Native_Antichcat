package com.example.nativeanticheat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/**
 * 服务端验签：必须与客户端 IntegrityChecker.sign 使用完全相同的
 * 密钥、算法和拼接顺序。
 */
public class IntegrityChecker {

    private static final String SHARED_SECRET = "native-anticheat-v1-shared-key";

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
}
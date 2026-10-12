package com.samplemall.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 极简 HS256 JWT（零第三方依赖）：用户服务签发，网关校验。
 * 载荷只放 sub（用户 ID）/ name（用户名，ASCII）/ exp（秒）。
 */
public final class JwtSupport {

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final String HEADER =
            ENC.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final Pattern SUB = Pattern.compile("\"sub\":\"(\\d+)\"");
    private static final Pattern NAME = Pattern.compile("\"name\":\"([A-Za-z0-9_.-]+)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");

    /** 令牌校验通过后的身份。 */
    public record Claims(long userId, String username, long expiresAt) {
    }

    private final byte[] secret;

    public JwtSupport(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(long userId, String username, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = "{\"sub\":\"" + userId + "\",\"name\":\"" + username + "\",\"exp\":" + exp + "}";
        String body = HEADER + "." + ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    /** 校验签名与有效期；任何异常都返回 null。 */
    public Claims verify(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        byte[] expected = sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            String payload = new String(DEC.decode(parts[1]), StandardCharsets.UTF_8);
            Matcher sub = SUB.matcher(payload);
            Matcher name = NAME.matcher(payload);
            Matcher exp = EXP.matcher(payload);
            if (!sub.find() || !name.find() || !exp.find()) {
                return null;
            }
            long expiresAt = Long.parseLong(exp.group(1));
            if (expiresAt < Instant.now().getEpochSecond()) {
                return null;
            }
            return new Claims(Long.parseLong(sub.group(1)), name.group(1), expiresAt);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return ENC.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }
}

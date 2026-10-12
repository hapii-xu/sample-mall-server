package com.samplemall.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 演示级口令摘要：SHA-256(salt + password)。生产环境应换成 BCrypt/Argon2。 */
final class PasswordHasher {

    private PasswordHasher() {
    }

    static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean matches(String salt, String password, String expectedHash) {
        return MessageDigest.isEqual(
                hash(salt, password).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}

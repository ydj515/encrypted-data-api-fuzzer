package com.example.gateway.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class Sha256ChecksumModule implements ChecksumModule {

    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final MessageDigest PROTOTYPE = createDigest();
    private static final ThreadLocal<MessageDigest> THREAD_LOCAL_DIGEST =
            ThreadLocal.withInitial(() -> cloneDigest(PROTOTYPE));

    @Override
    public String checksum(String encryptedData) {
        MessageDigest digest = THREAD_LOCAL_DIGEST.get();
        digest.reset();
        byte[] hash = digest.digest(encryptedData.getBytes(StandardCharsets.UTF_8));
        return HEX_FORMAT.formatHex(hash);
    }

    private static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static MessageDigest cloneDigest(MessageDigest prototype) {
        try {
            return (MessageDigest) prototype.clone();
        } catch (CloneNotSupportedException ignored) {
            // fallback to creating a new instance if clone is not supported
            return createDigest();
        }
    }
}

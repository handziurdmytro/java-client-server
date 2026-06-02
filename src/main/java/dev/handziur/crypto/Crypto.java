package dev.handziur.crypto;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class Crypto {
    private static final int KEY_SIZE = 128;
    private static final String ALGORITHM = "AES";
    private static final byte[] FIXED_KEY = "1111222233334444".getBytes(StandardCharsets.UTF_8);

    private final SecretKey key;

    public Crypto() {
        this.key = new SecretKeySpec(FIXED_KEY, ALGORITHM);
    }

    public byte[] encrypt(byte[] bytes) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(bytes);
    }

    public byte[] decrypt(byte[] bytes) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(bytes);
    }
}

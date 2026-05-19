package dev.handziur.crypto;

import javax.crypto.*;
import java.security.GeneralSecurityException;

public class Crypto {
    private static final int KEY_SIZE = 128;
    private static final String ALGORITHM = "AES";

    private final SecretKey key;

    public Crypto() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        this.key = keyGen.generateKey();
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

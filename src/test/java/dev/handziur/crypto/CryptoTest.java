package dev.handziur.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CryptoTest {

    private Crypto crypto;
    private static final String SAMPLE =
            "the rusted chains of prison moon are shuttered by the sun";

    @BeforeEach
    void setUp() throws Exception {
        crypto = new Crypto();
    }

    @Test
    void compareEncryptedAndDecryptedWithOriginal() throws Exception {
        byte[] original = SAMPLE.getBytes();

        byte[] encrypted = crypto.encrypt(original);
        byte[] decrypted = crypto.decrypt(encrypted);

        assertArrayEquals(original, decrypted);
    }

    @Test
    void compareEncryptedWithOriginal() throws Exception {
        byte[] original = SAMPLE.getBytes();
        byte[] encrypted = crypto.encrypt(original);

        assertFalse(Arrays.equals(original, encrypted));
    }
}

package dev.handziur.protocol;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EncoderDecoderTest {

    private static final String SAMPLE = """
            {"username": "Dmytro123","role":"customer"}""";

    private Encoder encoder;
    private Decoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        Crypto crypto = new Crypto();
        encoder = new Encoder(crypto);
        decoder = new Decoder(crypto);
    }

    @Test
    void compareEncodedAndDecodedWithOriginal() {
        byte[] data = SAMPLE.getBytes();
        Packet original = new Packet(
                (byte) 1,
                1234L,
                new Message(2, 444, data)
        );

        byte[] encoded = encoder.encode(original);
        Packet decoded = decoder.decode(encoded);

        assertEquals(original.source(), decoded.source());
        assertEquals(original.packetId(), decoded.packetId());
        assertEquals(original.message().type(), decoded.message().type());
        assertEquals(original.message().userId(), decoded.message().userId());
        assertArrayEquals(original.message().data(), decoded.message().data());
    }

    @Test
    void emptyPayload() {
        Packet original = new Packet((byte) 5, 999L, new Message(1, 10, new byte[0]));
        byte[] encoded = encoder.encode(original);
        Packet decoded = decoder.decode(encoded);

        assertArrayEquals(original.message().data(), decoded.message().data());
    }

    @Test
    void edgeValues() {
        Packet original = new Packet(
                Byte.MAX_VALUE,
                Long.MAX_VALUE,
                new Message(Integer.MAX_VALUE, Integer.MIN_VALUE, SAMPLE.getBytes())
        );

        byte[] encoded = encoder.encode(original);
        Packet decoded = decoder.decode(encoded);

        assertEquals(original.source(), decoded.source());
        assertEquals(original.packetId(), decoded.packetId());
        assertEquals(original.message().type(), decoded.message().type());
        assertEquals(original.message().userId(), decoded.message().userId());
    }

    @Test
    void nullArray() {
        assertThrows(ProtocolException.class, () -> decoder.decode(null));
    }

    @Test
    void tooShortArray() {
        byte[] bytes = new byte[10];
        assertThrows(ProtocolException.class, () -> decoder.decode(bytes));
    }

    @Test
    void incorrectMagicNumber() {
        byte[] bytes = new byte[20];
        bytes[0] = 0x66;

        assertThrows(ProtocolException.class, () -> decoder.decode(bytes));
    }

    @Test
    void incorrectHeaderCrc() {
        Packet packet = new Packet((byte) 0, 0L, new Message(0, 0, SAMPLE.getBytes()));

        byte[] encoded = encoder.encode(packet);
        encoded[14] = 0x00;

        assertThrows(ProtocolException.class, () -> decoder.decode(encoded));
    }

    @Test
    void incorrectMessageCrc() {
        Packet packet = new Packet((byte) 0, 0L, new Message(0, 0, SAMPLE.getBytes()));

        byte[] encoded = encoder.encode(packet);
        encoded[encoded.length - 1] = 0x00;

        assertThrows(ProtocolException.class, () -> decoder.decode(encoded));
    }

    @Test
    void wrongPayload() {
        Packet packet = new Packet((byte) 0, 0L, new Message(0, 0, SAMPLE.getBytes()));

        byte[] encoded = encoder.encode(packet);
        encoded[20] = 0x00;

        assertThrows(ProtocolException.class, () -> decoder.decode(encoded));
    }

    @Test
    void truncatedDataLength() {
        Packet packet = new Packet((byte) 0, 0L, new Message(0, 0, SAMPLE.getBytes()));

        byte[] encoded = encoder.encode(packet);
        byte[] truncated = Arrays.copyOf(encoded, encoded.length - 5);

        assertThrows(ProtocolException.class, () -> decoder.decode(truncated));
    }
}

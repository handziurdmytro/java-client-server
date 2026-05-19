package dev.handziur.protocol;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;

public class Decoder {
    private static final byte MAGIC_NUMBER = 0x13;

    private final Crypto crypto;

    public Decoder(Crypto crypto) {
        this.crypto = crypto;
    }

    public Packet decode(byte[] bytes) throws Exception {
        int offset = 0;

        if (bytes[offset++] != MAGIC_NUMBER)
            throw new Exception("invalid packet: wrong magic number");

        byte source = bytes[offset++];

        long packetId = readLong(bytes, offset);
        offset += 8;

        int packetLen = readInt(bytes, offset);
        offset += 4;

        short headerCrc16 = readShort(bytes, offset);
        if (Crc16.calculateCrc(bytes, 0, offset) != headerCrc16)
            throw new Exception("invalid packet: wrong header checksum");
        offset += 2;

        int type = readInt(bytes, offset);
        offset += 4;

        int userId = readInt(bytes, offset);
        offset += 4;

        int dataLen = packetLen - 8;
        byte[] data = new byte[dataLen];
        for (int i = offset, j = 0; i < offset + dataLen; i++, j++) {
            data[j] = bytes[i];
        }
        offset += dataLen;

        byte[] decryptedData = crypto.decrypt(data);

        short messageCrc16 = readShort(bytes, offset);
        if (Crc16.calculateCrc(bytes, 16, packetLen) != messageCrc16)
            throw new Exception("invalid packet: wrong message checksum");

        return new Packet(
                source,
                packetId,
                new Message(type, userId, decryptedData)
        );
    }

    private short readShort(byte[] bytes, int offset) {
        return (short) (((bytes[offset] & 0xFF) << 8) |
                ((bytes[offset + 1] & 0xFF)));
    }

    private int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
                ((bytes[offset + 1] & 0xFF) << 16) |
                ((bytes[offset + 2] & 0xFF) << 8) |
                ((bytes[offset + 3] & 0xFF));
    }

    private long readLong(byte[] bytes, int offset) {
        return ((long) (bytes[offset] & 0xFF) << 56) |
                ((long) (bytes[offset + 1] & 0xFF) << 48) |
                ((long) (bytes[offset + 2] & 0xFF) << 40) |
                ((long) (bytes[offset + 3] & 0xFF) << 32) |
                ((long) (bytes[offset + 4] & 0xFF) << 24) |
                ((long) (bytes[offset + 5] & 0xFF) << 16) |
                ((long) (bytes[offset + 6] & 0xFF) << 8) |
                ((long) (bytes[offset + 7] & 0xFF));
    }

}


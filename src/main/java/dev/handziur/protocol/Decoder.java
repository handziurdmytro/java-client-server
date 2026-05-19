package dev.handziur.protocol;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;

import java.security.GeneralSecurityException;

public class Decoder {
    private static final byte MAGIC_NUMBER = 0x13;

    private final Crypto crypto;

    public Decoder(Crypto crypto) {
        this.crypto = crypto;
    }

    public Packet decode(byte[] bytes) throws ProtocolException {
        if (bytes == null || bytes.length < 16)
            throw new ProtocolException("invalid packet: short header");

        int offset = 0;

        if (bytes[offset++] != MAGIC_NUMBER)
            throw new ProtocolException("invalid packet: wrong magic number");

        byte source = bytes[offset++];

        long packetId = readLong(bytes, offset);
        offset += 8;

        int wLen = readInt(bytes, offset);
        offset += 4;

        if (wLen < 0 || bytes.length < 16 + wLen + 2)
            throw new ProtocolException("invalid packet: invalid data length");

        short headerCrc16 = readShort(bytes, offset);
        if (Crc16.calculateCrc(bytes, 0, offset) != headerCrc16) {
            throw new ProtocolException("invalid packet: wrong header checksum");
        }
        offset += 2;

        byte[] encryptedMsg = new byte[wLen];
        for (int i = 0; i < wLen; i++) {
            encryptedMsg[i] = bytes[offset + i];
        }
        offset += wLen;

        short messageCrc16 = readShort(bytes, offset);
        if (Crc16.calculateCrc(bytes, 16, wLen) != messageCrc16) {
            throw new ProtocolException("invalid packet: wrong message checksum");
        }

        try {
            byte[] unencryptedMsg = crypto.decrypt(encryptedMsg);
            if (unencryptedMsg.length < 8) {
                throw new ProtocolException("invalid decrypted message: wrong structure");
            }

            int type = readInt(unencryptedMsg, 0);
            int userId = readInt(unencryptedMsg, 4);

            int dataLen = unencryptedMsg.length - 8;
            byte[] data = new byte[dataLen];
            for (int i = 0; i < dataLen; i++) {
                data[i] = unencryptedMsg[8 + i];
            }

            return new Packet(source, packetId, new Message(type, userId, data));
        } catch (GeneralSecurityException e) {
            throw new ProtocolException("packet decoding and decryption failed", e);
        }
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


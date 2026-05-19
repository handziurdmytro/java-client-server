package dev.handziur.protocol;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Packet;
import java.security.GeneralSecurityException;

public class Encoder {

    private static final byte MAGIC_NUMBER = 0x13;

    private final Crypto crypto;

    public Encoder(Crypto crypto) {
        this.crypto = crypto;
    }

    public byte[] encode(Packet packet) throws ProtocolException {
        try {
            byte[] rawData = packet.message().data();
            byte[] unencryptedMsg = new byte[4 + 4 + rawData.length];

            writeInt(unencryptedMsg, 0, packet.message().type());
            writeInt(unencryptedMsg, 4, packet.message().userId());

            for (int i = 0; i < rawData.length; i++) {
                unencryptedMsg[8 + i] = rawData[i];
            }

            byte[] encryptedMsg = crypto.encrypt(unencryptedMsg);
            int wLen = encryptedMsg.length;
            int totalLen = 16 + wLen + 2;

            byte[] bytes = new byte[totalLen];
            int offset = 0;

            bytes[offset++] = MAGIC_NUMBER;
            bytes[offset++] = packet.source();

            writeLong(bytes, offset, packet.packetId());
            offset += 8;

            writeInt(bytes, offset, wLen);
            offset += 4;

            short headerCrc16 = Crc16.calculateCrc(bytes, 0, offset);
            writeShort(bytes, offset, headerCrc16);
            offset += 2;

            for (int i = 0; i < wLen; i++) {
                bytes[offset++] = encryptedMsg[i];
            }

            short messageCrc16 = Crc16.calculateCrc(bytes, 16, wLen);
            writeShort(bytes, offset, messageCrc16);

            return bytes;
        } catch (GeneralSecurityException e) {
            throw new ProtocolException("packet encoding and encryption failed", e);
        }
    }

    private void writeShort(byte[] bytes, int offset, short val) {
        bytes[offset] = (byte) (val >> 8);
        bytes[offset + 1] = (byte) val;
    }

    private void writeInt(byte[] bytes, int offset, int val) {
        bytes[offset] = (byte) (val >> 24);
        bytes[offset + 1] = (byte) (val >> 16);
        bytes[offset + 2] = (byte) (val >> 8);
        bytes[offset + 3] = (byte) val;
    }

    private void writeLong(byte[] bytes, int offset, long val) {
        for (int i = 0; i < 8; i++) {
            bytes[offset + i] = (byte) (val >> (56 - i * 8));
        }
    }
}

package dev.handziur.protocol;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Packet;

public class Encoder {

    private static final byte MAGIC_NUMBER = 0x13;

    private final Crypto crypto;

    public Encoder(Crypto crypto) {
        this.crypto = crypto;
    }

    public byte[] encode(Packet packet) throws Exception {
        int offset = 0;

        byte[] encryptedData = crypto.encrypt(packet.message().data());
        int dataLen = encryptedData.length;
        int len = 1 + 1 + 8 + 4 + 2 + (4 + 4 + dataLen) + 2;

        byte[] bytes = new byte[len];

        bytes[offset++] = MAGIC_NUMBER;
        bytes[offset++] = packet.source();

        writeLong(bytes, offset, packet.packetId());
        offset += 8;

        writeInt(bytes, offset, 4 + 4 + dataLen);
        offset += 4;

        short headerCrc16 = Crc16.calculateCrc(bytes, 0, offset);
        writeShort(bytes, offset, headerCrc16);
        offset += 2;

        writeInt(bytes, offset, packet.message().type());
        offset += 4;

        writeInt(bytes, offset, packet.message().userId());
        offset += 4;



        for (byte b : encryptedData) {
            bytes[offset++] = b;
        }

        short messageCrc16 = Crc16.calculateCrc(bytes, 16, 4 + 4 + dataLen);
        writeShort(bytes, offset, messageCrc16);

        return bytes;
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

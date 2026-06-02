package dev.handziur.network;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class StoreClientTCP {
    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 3131;

    public static void main(String[] args) throws Exception {
        Crypto crypto = new Crypto();
        Encoder encoder = new Encoder(crypto);
        Decoder decoder = new Decoder(crypto);

        long packetId = 1;
        String[] albums = {"Mezzanine", "Dummy", "Treasure"};

        while (true) {
            try (Socket socket = new Socket(ADDRESS, PORT);
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                System.out.println("[" + LocalDateTime.now() + "][INFO] connected to server at " + ADDRESS + ":" + PORT);

                for (String album : albums) {
                    String payload = album + ":500";
                    Message msg = new Message(PacketType.ADD_ITEM_QTY.code(), 1, payload.getBytes(StandardCharsets.UTF_8));
                    Packet request = new Packet((byte) 1, packetId++, msg);

                    out.write(encoder.encode(request));
                    out.flush();

                    byte[] rawResponse = readFullPacket(in);
                    Packet response = decoder.decode(rawResponse);

                    String responseStr = new String(response.message().data(), StandardCharsets.UTF_8);
                    System.out.printf("[" + LocalDateTime.now() + "][INFO] sent: %-18s; received: %s%n", payload, responseStr);
                    Thread.sleep(1000);
                }
                break;

            } catch (IOException e) {
                System.err.println("[" + LocalDateTime.now() + "][WARN] Connection lost or refused. Retrying in 3s...");
                Thread.sleep(3000);
            }
        }
    }

    private static byte[] readFullPacket(InputStream in) throws IOException {
        byte[] header = new byte[16];
        readFully(in, header, 16);

        int wLen = ((header[10] & 0xFF) << 24) |
                ((header[11] & 0xFF) << 16) |
                ((header[12] & 0xFF) << 8) |
                ((header[13] & 0xFF));

        byte[] tail = new byte[wLen + 2];
        readFully(in, tail, wLen + 2);

        byte[] full = new byte[16 + wLen + 2];
        for (int i = 0; i < 16; i++) {
            full[i] = header[i];
        }
        for (int i = 0; i < wLen + 2; i++) {
            full[16 + i] = tail[i];
        }

        return full;
    }

    private static void readFully(InputStream in, byte[] buf, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int count = in.read(buf, total, len - total);
            if (count == -1) throw new EOFException();
            total += count;
        }
    }
}

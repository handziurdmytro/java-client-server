package dev.handziur.network;

import dev.handziur.crypto.Crypto;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class StoreClientUDP {
    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 3232;
    private static final int TIMEOUT_MS = 500;
    private static final int MAX_RETRIES = 3;

    public static void main(String[] args) throws Exception {
        Crypto crypto = new Crypto();
        Encoder encoder = new Encoder(crypto);
        Decoder decoder = new Decoder(crypto);

        InetAddress address = InetAddress.getByName(ADDRESS);
        long packetId = 1;
        String[] albums = {"Blue Lines", "Protection", "Heligoland"};

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            for (String album : albums) {
                String payload = album + ":1000";
                Message msg = new Message(PacketType.ADD_ITEM_QTY.code(), 1, payload.getBytes(StandardCharsets.UTF_8));
                Packet request = new Packet((byte) 1, packetId++, msg);
                byte[] rawRequest = encoder.encode(request);

                boolean success = false;
                int retries = 0;

                while (retries < MAX_RETRIES && !success) {
                    try {
                        DatagramPacket outPacket = new DatagramPacket(rawRequest, rawRequest.length, address, PORT);
                        socket.send(outPacket);

                        byte[] buffer = new byte[2048];
                        DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(inPacket);

                        byte[] rawResponse = new byte[inPacket.getLength()];
                        for (int j = 0; j < inPacket.getLength(); j++) {
                            rawResponse[j] = inPacket.getData()[j];
                        }

                        Packet response = decoder.decode(rawResponse);
                        String responseStr = new String(response.message().data(), StandardCharsets.UTF_8);
                        System.out.printf("[" + LocalDateTime.now() + "][INFO] sent: %-18s; received: %s%n", payload, responseStr);
                        success = true;

                    } catch (SocketTimeoutException e) {
                        retries++;
                        System.err.printf("[" + LocalDateTime.now() + "][WARN] packet timeout; retry %d/%d%n", retries, MAX_RETRIES);
                    }
                }

                if (!success) {
                    System.err.printf("[" + LocalDateTime.now() + "][ERROR] failed to deliver payload: %s%n", payload);
                }

                Thread.sleep(1000);
            }
        } catch (IOException e) {
            System.err.println("[" + LocalDateTime.now() + "][ERROR] network initialization failed: " + e.getMessage());
        }
    }
}

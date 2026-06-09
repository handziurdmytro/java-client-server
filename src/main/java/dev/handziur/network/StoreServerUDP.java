package dev.handziur.network;

import dev.handziur.crypto.Crypto;
import dev.handziur.domain.ProductService;
import dev.handziur.model.Packet;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;
import dev.handziur.protocol.Processor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.util.Arrays;

public class StoreServerUDP {
    private static final int PORT = 3232;
    private static final int MAX_BUFFER = 1024;

    public static void main(String[] args) throws Exception {
        Crypto crypto = new Crypto();
        Encoder encoder = new Encoder(crypto);
        Decoder decoder = new Decoder(crypto);
        Processor processor = new Processor(new ProductService("jdbc:sqlite:warehouse.db"));

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("[" + LocalDateTime.now() + "][INFO] UDP Server listening on port " + PORT);
            byte[] buffer = new byte[MAX_BUFFER];

            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(incomingPacket);

                String clientAddress = incomingPacket.getAddress().getHostAddress() + ":" + incomingPacket.getPort();
                System.out.println("[" + LocalDateTime.now() + "][INFO] Received datagram from " + clientAddress);

                try {
                    byte[] rawData = Arrays.copyOf(incomingPacket.getData(), incomingPacket.getLength());
                    Packet request = decoder.decode(rawData);

                    Packet response = processor.process(request);
                    byte[] rawResponse = encoder.encode(response);

                    DatagramPacket outgoingPacket = new DatagramPacket(
                            rawResponse,
                            rawResponse.length,
                            incomingPacket.getAddress(),
                            incomingPacket.getPort()
                    );
                    socket.send(outgoingPacket);
                    System.out.println("[" + LocalDateTime.now() + "][INFO] Response successfully sent to " + clientAddress);

                } catch (Exception e) {
                    System.err.println("[" + LocalDateTime.now() + "][ERROR] Failed to process datagram from " + clientAddress + ": " + e.getMessage());
                }
            }
        }
    }
}

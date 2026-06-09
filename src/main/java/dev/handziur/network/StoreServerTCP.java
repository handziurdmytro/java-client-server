package dev.handziur.network;

import dev.handziur.crypto.Crypto;
import dev.handziur.domain.ProductService;
import dev.handziur.model.Packet;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;
import dev.handziur.protocol.Processor;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerTCP {
    private static final int PORT = 3131;

    public static void main(String[] args) throws Exception {
        Crypto crypto = new Crypto();
        Encoder encoder = new Encoder(crypto);
        Decoder decoder = new Decoder(crypto);
        Processor processor = new Processor(new ProductService("jdbc:sqlite:warehouse.db"));

        ExecutorService pool = Executors.newFixedThreadPool(8);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[INFO] TCP Server listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.submit(() -> handleClient(clientSocket, decoder, processor, encoder));
            }
        }
    }

    private static void handleClient(Socket socket, Decoder decoder, Processor processor, Encoder encoder) {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        System.out.println("[" + LocalDateTime.now() + "][INFO] Client connected: " + clientAddress);

        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            while (!socket.isClosed()) {
                byte[] rawRequest = readFullPacket(in);
                Packet request = decoder.decode(rawRequest);

                String requestStr = new String(request.message().data(), StandardCharsets.UTF_8);
                System.out.printf("[" + LocalDateTime.now() + "][INFO] Received from %s: %s%n", clientAddress, requestStr);

                Packet response = processor.process(request);
                byte[] rawResponse = encoder.encode(response);

                String responseStr = new String(response.message().data(), StandardCharsets.UTF_8);

                out.write(rawResponse);
                out.flush();

                System.out.printf("[" + LocalDateTime.now() + "][INFO] Sent to %s: %s%n", clientAddress, responseStr);
            }
        } catch (EOFException e) {
            System.out.println("[" + LocalDateTime.now() + "][INFO] Client disconnected: " + clientAddress);
        } catch (Exception e) {
            System.err.println("[" + LocalDateTime.now() + "][ERROR] Connection dropped (" + clientAddress + "): " + e.getMessage());
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
            if (count == -1) {
                throw new EOFException();
            }
            total += count;
        }
    }
}

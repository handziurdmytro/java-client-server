package dev.handziur.core;

import dev.handziur.network.Receiver;
import dev.handziur.network.Sender;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;
import dev.handziur.protocol.Processor;
import dev.handziur.model.Packet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private final BlockingQueue<byte[]> rawInputQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> decodedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> processedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> rawOutputQueue = new LinkedBlockingQueue<>();

    private final ExecutorService receiverPool;
    private final ExecutorService decryptorPool;
    private final ExecutorService processorPool;
    private final ExecutorService encryptorPool;
    private final ExecutorService senderPool;

    private final int receiverCount;
    private final int decryptorCount;
    private final int processorCount;
    private final int encryptorCount;
    private final int senderCount;

    private final Receiver receiver;
    private final Decoder decoder;
    private final Processor processor;
    private final Encoder encoder;
    private final Sender sender;

    public Server(int receiverCount, int decryptorCount, int processorCount, int encryptorCount, int senderCount,
                  Receiver receiver, Decoder decoder, Processor processor,
                  Encoder encoder, Sender sender) {
        this.receiverCount = receiverCount;
        this.decryptorCount = decryptorCount;
        this.processorCount = processorCount;
        this.encryptorCount = encryptorCount;
        this.senderCount = senderCount;

        this.receiverPool = Executors.newFixedThreadPool(receiverCount);
        this.decryptorPool = Executors.newFixedThreadPool(decryptorCount);
        this.processorPool = Executors.newFixedThreadPool(processorCount);
        this.encryptorPool = Executors.newFixedThreadPool(encryptorCount);
        this.senderPool = Executors.newFixedThreadPool(senderCount);

        this.receiver = receiver;
        this.decoder = decoder;
        this.processor = processor;
        this.encoder = encoder;
        this.sender = sender;
    }

    public void start() {
        startWorkers(receiverPool, receiverCount, () -> {
            byte[] data = receiver.receive();
            rawInputQueue.put(data);
        });

        startWorkers(decryptorPool, decryptorCount, () -> {
            byte[] raw = rawInputQueue.take();
            Packet packet = decoder.decode(raw);
            decodedQueue.put(packet);
        });

        startWorkers(processorPool, processorCount, () -> {
            Packet req = decodedQueue.take();
            Packet res = processor.process(req);
            processedQueue.put(res);
        });

        startWorkers(encryptorPool, encryptorCount, () -> {
            Packet res = processedQueue.take();
            byte[] encoded = encoder.encode(res);
            rawOutputQueue.put(encoded);
        });

        startWorkers(senderPool, senderCount, () -> {
            byte[] data = rawOutputQueue.take();
            sender.send(data);
        });
    }

    private void startWorkers(ExecutorService pool, int threadCount, InterruptibleAction action) {
        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        action.execute();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        System.err.println("Worker exception: " + e.getMessage());
                    }
                }
            });
        }
    }

    public void shutdown() {
        receiverPool.shutdownNow();
        decryptorPool.shutdownNow();
        processorPool.shutdownNow();
        encryptorPool.shutdownNow();
        senderPool.shutdownNow();

        try {
            receiverPool.awaitTermination(2, TimeUnit.SECONDS);
            decryptorPool.awaitTermination(2, TimeUnit.SECONDS);
            processorPool.awaitTermination(2, TimeUnit.SECONDS);
            encryptorPool.awaitTermination(2, TimeUnit.SECONDS);
            senderPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface InterruptibleAction {
        void execute() throws Exception;
    }
}
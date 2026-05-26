package dev.handziur.network;

import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;
import dev.handziur.protocol.Encoder;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class MockReceiver implements Receiver {
    private final Encoder encoder;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Random random = new Random();

    private static final String[] ALBUMS = {
            "Mezzanine", "Dummy", "Treasure", "Third",
            "Blue Lines", "Protection", "Heligoland"
    };

    private static final String[] BANDS = {
            "Massive Attack", "Portishead", "Cocteau Twins", "Trip-Hop"
    };

    public MockReceiver(Encoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public byte[] receive() throws InterruptedException {
        Thread.sleep(random.nextInt(100) + 50);

        PacketType[] types = PacketType.values();
        PacketType type = types[random.nextInt(types.length)];

        String album = ALBUMS[random.nextInt(ALBUMS.length)];
        String band = BANDS[random.nextInt(BANDS.length)];
        int amount = random.nextInt(1000) + 1;

        String payload;
        switch (type) {
            case GET_ITEM_QTY -> payload = album;
            case ADD_ITEM_QTY, REMOVE_ITEM_QTY -> payload = album + ":" + amount;
            case CREATE_GROUP -> payload = band;
            case ADD_GROUP_NAME -> payload = band + ":" + album;
            case SET_ITEM_PRICE -> payload = album + ":" + (amount + 0.99);
            default -> payload = album;
        }

        Message msg = new Message(type.code(), 1, payload.getBytes(StandardCharsets.UTF_8));
        Packet p = new Packet((byte) 1, idGenerator.getAndIncrement(), msg);

        try {
            return encoder.encode(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode fake packet", e);
        }
    }
}
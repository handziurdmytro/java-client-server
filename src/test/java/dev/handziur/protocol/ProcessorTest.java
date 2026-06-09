package dev.handziur.protocol;

import dev.handziur.domain.Product;
import dev.handziur.domain.ProductFilterParams;
import dev.handziur.domain.ProductService;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessorTest {

    private ProductService dbService;
    private Processor processor;
    private File tempDb;

    @BeforeEach
    void setUp() throws Exception {
        tempDb = File.createTempFile("test_warehouse", ".db");
        dbService = new ProductService("jdbc:sqlite:" + tempDb.getAbsolutePath() + "?busy_timeout=5000");
        processor = new Processor(dbService);
    }

    @AfterEach
    void tearDown() {
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    private Packet createRequest(PacketType type, String payload) {
        Message message = new Message(type.code(), 1, payload.getBytes(StandardCharsets.UTF_8));
        return new Packet((byte) 1, 100L, message);
    }

    private String extractPayload(Packet packet) {
        return new String(packet.message().data(), StandardCharsets.UTF_8);
    }

    private int getQty(String name) {
        List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
        return res.isEmpty() ? 0 : res.get(0).quantity();
    }

    private String getCategory(String name) {
        List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
        return res.isEmpty() ? null : res.get(0).category();
    }

    private double getPrice(String name) {
        List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
        return res.isEmpty() ? 0.0 : res.get(0).price();
    }

    @Test
    void processAddItemQty() {
        Packet request = createRequest(PacketType.ADD_ITEM_QTY, "Dummy:500000");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
        assertEquals(500000, getQty("Dummy"));
    }

    @Test
    void processGetItemQty() {
        dbService.create(new Product(0, "Portishead", "Uncategorized", 300000, 0.0));
        Packet request = createRequest(PacketType.GET_ITEM_QTY, "Portishead");
        Packet response = processor.process(request);

        assertEquals("OK:300000", extractPayload(response));
    }

    @Test
    void processRemoveItemQtySuccess() {
        dbService.create(new Product(0, "Mezzanine", "Uncategorized", 1000000, 0.0));
        Packet request = createRequest(PacketType.REMOVE_ITEM_QTY, "Mezzanine:400000");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
        assertEquals(600000, getQty("Mezzanine"));
    }

    @Test
    void processRemoveItemQtyFail() {
        dbService.create(new Product(0, "100th Window", "Uncategorized", 200000, 0.0));
        Packet request = createRequest(PacketType.REMOVE_ITEM_QTY, "100th Window:400000");
        Packet response = processor.process(request);

        assertEquals("ERROR:Not enough quantity", extractPayload(response));
        assertEquals(200000, getQty("100th Window"));
    }

    @Test
    void processCreateGroup() {
        Packet request = createRequest(PacketType.CREATE_GROUP, "Massive Attack");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
    }

    @Test
    void processAddGroupNameSuccess() {
        dbService.create(new Product(0, "Treasure", "Uncategorized", 10, 0.0));
        Packet request = createRequest(PacketType.ADD_GROUP_NAME, "Cocteau Twins:Treasure");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
        assertEquals("Cocteau Twins", getCategory("Treasure"));
    }

    @Test
    void processSetItemPrice() {
        Packet request = createRequest(PacketType.SET_ITEM_PRICE, "The Moon & Antarctica:35000.50");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
        assertEquals(35000.50, getPrice("The Moon & Antarctica"));
    }

    @Test
    void processInvalidPayloadFormat() {
        Packet request = createRequest(PacketType.ADD_ITEM_QTY, "Third_Without_Amount");
        Packet response = processor.process(request);

        assertEquals("ERROR:Invalid payload format or unknown command", extractPayload(response));
    }

    @Test
    void processInvalidCommand() {
        Message message = new Message(999, 1, "Data".getBytes(StandardCharsets.UTF_8));
        Packet request = new Packet((byte) 1, 100L, message);
        Packet response = processor.process(request);

        assertEquals("ERROR:Invalid payload format or unknown command", extractPayload(response));
    }

    @Test
    void concurrentProcessRequests() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                Packet request = createRequest(PacketType.ADD_ITEM_QTY, "Heligoland:2000");
                processor.process(request);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(200000, getQty("Heligoland"));
    }
}

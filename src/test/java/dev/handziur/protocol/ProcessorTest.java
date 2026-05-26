package dev.handziur.protocol;

import dev.handziur.domain.ConcurrentWarehouse;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {

    private ConcurrentWarehouse warehouse;
    private Processor processor;

    @BeforeEach
    void setUp() {
        warehouse = new ConcurrentWarehouse();
        processor = new Processor(warehouse);
    }

    private Packet createRequest(PacketType type, String payload) {
        Message message = new Message(type.code(), 1, payload.getBytes(StandardCharsets.UTF_8));
        return new Packet((byte) 1, 100L, message);
    }

    private String extractPayload(Packet packet) {
        return new String(packet.message().data(), StandardCharsets.UTF_8);
    }

    @Test
    void processAddItemQty() {
        Packet request = createRequest(PacketType.ADD_ITEM_QTY, "Dummy:500000");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
        assertEquals(500000, warehouse.getItemQty("Dummy"));
    }

    @Test
    void processGetItemQty() {
        warehouse.addItemQty("Portishead", 300000);
        Packet request = createRequest(PacketType.GET_ITEM_QTY, "Portishead");
        Packet response = processor.process(request);

        assertEquals("OK:300000", extractPayload(response));
    }

    @Test
    void processRemoveItemQtySuccess() {
        warehouse.addItemQty("Mezzanine", 1000000);
        Packet request = createRequest(PacketType.REMOVE_ITEM_QTY, "Mezzanine:400000");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
        assertEquals(600000, warehouse.getItemQty("Mezzanine"));
    }

    @Test
    void processRemoveItemQtyFail() {
        warehouse.addItemQty("100th Window", 200000);
        Packet request = createRequest(PacketType.REMOVE_ITEM_QTY, "100th Window:400000");
        Packet response = processor.process(request);

        assertEquals("ERROR:Not enough quantity", extractPayload(response));
        assertEquals(200000, warehouse.getItemQty("100th Window"));
    }

    @Test
    void processCreateGroup() {
        Packet request = createRequest(PacketType.CREATE_GROUP, "Massive Attack");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
    }

    @Test
    void processCreateGroupDuplicate() {
        warehouse.createGroup("Massive Attack");
        Packet request = createRequest(PacketType.CREATE_GROUP, "Massive Attack");
        Packet response = processor.process(request);

        assertEquals("ERROR:Group already exists", extractPayload(response));
    }

    @Test
    void processAddGroupNameSuccess() {
        warehouse.createGroup("Cocteau Twins");
        Packet request = createRequest(PacketType.ADD_GROUP_NAME, "Cocteau Twins:Treasure");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
    }

    @Test
    void processAddGroupNameFail() {
        Packet request = createRequest(PacketType.ADD_GROUP_NAME, "Modest Mouse:Good News");
        Packet response = processor.process(request);

        assertEquals("ERROR:Group does not exist", extractPayload(response));
    }

    @Test
    void processSetItemPrice() {
        Packet request = createRequest(PacketType.SET_ITEM_PRICE, "The Moon & Antarctica:35000.50");
        Packet response = processor.process(request);

        assertEquals("OK", extractPayload(response));
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
        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 0; i < 500; i++) {
            executor.submit(() -> {
                Packet request = createRequest(PacketType.ADD_ITEM_QTY, "Heligoland:2000");
                processor.process(request);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1000000, warehouse.getItemQty("Heligoland"));
    }
}
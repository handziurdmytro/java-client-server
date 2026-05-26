package dev.handziur.protocol;

import dev.handziur.domain.ConcurrentWarehouse;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;

import java.nio.charset.StandardCharsets;

public class Processor {
    private final ConcurrentWarehouse warehouse;

    public Processor(ConcurrentWarehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Packet process(Packet requestPacket) {
        Message reqMsg = requestPacket.message();
        String responsePayload;

        try {
            PacketType command = PacketType.fromCode(reqMsg.type());
            String payloadStr = new String(reqMsg.data(), StandardCharsets.UTF_8);
            String[] parts = payloadStr.split(":");

            switch (command) {
                case GET_ITEM_QTY -> {
                    int qty = warehouse.getItemQty(parts[0]);
                    responsePayload = "OK:" + qty;
                }
                case ADD_ITEM_QTY -> {
                    warehouse.addItemQty(parts[0], Integer.parseInt(parts[1]));
                    responsePayload = "OK";
                }
                case REMOVE_ITEM_QTY -> {
                    boolean success = warehouse.removeItemQty(parts[0], Integer.parseInt(parts[1]));
                    responsePayload = success ? "OK" : "ERROR:Not enough quantity";
                }
                case CREATE_GROUP -> {
                    boolean created = warehouse.createGroup(parts[0]);
                    responsePayload = created ? "OK" : "ERROR:Group already exists";
                }
                case ADD_GROUP_NAME -> {
                    boolean added = warehouse.addGroupName(parts[0], parts[1]);
                    responsePayload = added ? "OK" : "ERROR:Group does not exist";
                }
                case SET_ITEM_PRICE -> {
                    warehouse.setItemPrice(parts[0], Double.parseDouble(parts[1]));
                    responsePayload = "OK";
                }
                default -> responsePayload = "ERROR:Unknown command";
            }
        } catch (Exception e) {
            responsePayload = "ERROR:Invalid payload format or unknown command";
        }

        Message responseMsg = new Message(
                reqMsg.type(),
                reqMsg.userId(),
                responsePayload.getBytes(StandardCharsets.UTF_8)
        );

        return new Packet(requestPacket.source(), requestPacket.packetId(), responseMsg);
    }
}

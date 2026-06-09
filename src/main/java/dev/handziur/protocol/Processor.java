package dev.handziur.protocol;

import dev.handziur.domain.Product;
import dev.handziur.domain.ProductFilterParams;
import dev.handziur.domain.ProductService;
import dev.handziur.model.Message;
import dev.handziur.model.Packet;
import dev.handziur.model.PacketType;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Processor {
    private final ProductService dbService;

    public Processor(ProductService dbService) {
        this.dbService = dbService;
    }

    public synchronized Packet process(Packet requestPacket) {
        Message reqMsg = requestPacket.message();
        String responsePayload;

        try {
            PacketType command = PacketType.fromCode(reqMsg.type());
            String payloadStr = new String(reqMsg.data(), StandardCharsets.UTF_8);
            String[] parts = payloadStr.split(":");

            switch (command) {
                case GET_ITEM_QTY -> {
                    List<Product> res = dbService.search(new ProductFilterParams(parts[0], null, null, null, null, null), 1, 0);
                    responsePayload = res.isEmpty() ? "OK:0" : "OK:" + res.get(0).quantity();
                }
                case ADD_ITEM_QTY -> {
                    String name = parts[0];
                    int amount = Integer.parseInt(parts[1]);
                    List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
                    if (res.isEmpty()) {
                        dbService.create(new Product(0, name, "Uncategorized", amount, 0.0));
                    } else {
                        Product p = res.get(0);
                        dbService.update(new Product(p.id(), p.name(), p.category(), p.quantity() + amount, p.price()));
                    }
                    responsePayload = "OK";
                }
                case REMOVE_ITEM_QTY -> {
                    String name = parts[0];
                    int amount = Integer.parseInt(parts[1]);
                    List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
                    if (res.isEmpty() || res.get(0).quantity() < amount) {
                        responsePayload = "ERROR:Not enough quantity";
                    } else {
                        Product p = res.get(0);
                        dbService.update(new Product(p.id(), p.name(), p.category(), p.quantity() - amount, p.price()));
                        responsePayload = "OK";
                    }
                }
                case CREATE_GROUP -> {
                    responsePayload = "OK";
                }
                case ADD_GROUP_NAME -> {
                    String group = parts[0];
                    String name = parts[1];
                    List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
                    if (res.isEmpty()) {
                        dbService.create(new Product(0, name, group, 0, 0.0));
                    } else {
                        Product p = res.get(0);
                        dbService.update(new Product(p.id(), p.name(), group, p.quantity(), p.price()));
                    }
                    responsePayload = "OK";
                }
                case SET_ITEM_PRICE -> {
                    String name = parts[0];
                    double price = Double.parseDouble(parts[1]);
                    List<Product> res = dbService.search(new ProductFilterParams(name, null, null, null, null, null), 1, 0);
                    if (res.isEmpty()) {
                        dbService.create(new Product(0, name, "Uncategorized", 0, price));
                    } else {
                        Product p = res.get(0);
                        dbService.update(new Product(p.id(), p.name(), p.category(), p.quantity(), price));
                    }
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

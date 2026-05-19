package dev.handziur.model;

public record Packet(
        byte source,
        long packetId,
        Message message
) {

}

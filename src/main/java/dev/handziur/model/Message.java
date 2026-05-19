package dev.handziur.model;

public record Message(
        int type,
        int userId,
        byte[] data
) {

}

package dev.handziur.network;

public interface Sender {
    void send(byte[] data) throws InterruptedException;
}

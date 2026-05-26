package dev.handziur.network;

public interface Receiver {
    byte[] receive() throws InterruptedException;
}
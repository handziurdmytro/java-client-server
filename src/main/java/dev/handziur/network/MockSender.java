package dev.handziur.network;

public class MockSender implements Sender {
    @Override
    public void send(byte[] data) {
        System.out.println("Packet was sent (" + data.length + " bytes)");
    }
}

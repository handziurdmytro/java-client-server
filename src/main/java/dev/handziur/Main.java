package dev.handziur;

import dev.handziur.core.Server;
import dev.handziur.crypto.Crypto;
import dev.handziur.domain.ConcurrentWarehouse;
import dev.handziur.network.MockReceiver;
import dev.handziur.network.MockSender;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;
import dev.handziur.protocol.Processor;

public class Main {
    public static void main(String[] args) throws Exception {
        Crypto crypto = new Crypto();
        Encoder encoder = new Encoder(crypto);
        Decoder decoder = new Decoder(crypto);

        ConcurrentWarehouse warehouse = new ConcurrentWarehouse();
        Processor processor = new Processor(warehouse);

        MockReceiver receiver = new MockReceiver(encoder);
        MockSender sender = new MockSender();

        Server pipeline = new Server(
                2, 2, 4, 3, 5,
                receiver, decoder, processor, encoder, sender
        );

        System.out.println("Starting server pipeline...");
        pipeline.start();

        Thread.sleep(5000);

        System.out.println("Initiating graceful shutdown...");
        pipeline.shutdown();
        System.out.println("Server stopped gracefully.");

        System.out.println("Final 'Mezzanine' Qty: " + warehouse.getItemQty("Mezzanine"));
    }
}

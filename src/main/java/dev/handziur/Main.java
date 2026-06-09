package dev.handziur;

import dev.handziur.core.Server;
import dev.handziur.crypto.Crypto;
import dev.handziur.domain.ProductFilterParams;
import dev.handziur.domain.ProductService;
import dev.handziur.domain.Product;
import dev.handziur.network.MockReceiver;
import dev.handziur.network.MockSender;
import dev.handziur.protocol.Decoder;
import dev.handziur.protocol.Encoder;
import dev.handziur.protocol.Processor;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Crypto crypto = new Crypto();
        Encoder encoder = new Encoder(crypto);
        Decoder decoder = new Decoder(crypto);

        ProductService productService = new ProductService("jdbc:sqlite:warehouse.db");
        Processor processor = new Processor(productService);

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

        List<Product> res = productService.search(new ProductFilterParams("Mezzanine", null, null, null, null, null), 1, 0);
        int finalQty = res.isEmpty() ? 0 : res.get(0).quantity();
        System.out.println("Final 'Mezzanine' Qty: " + finalQty);
    }
}

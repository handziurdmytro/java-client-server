package dev.handziur;

import dev.handziur.domain.ProductService;
import dev.handziur.network.http.StoreServerHTTP;

public class Main {
    private static final short PORT = 2929;
    private static final String DB_URL = "jdbc:sqlite:store.db";

    public static void main(String[] args) throws Exception {
        ProductService productService = new ProductService(DB_URL);
        StoreServerHTTP httpServer = new StoreServerHTTP(productService, PORT);
        httpServer.start();
    }
}

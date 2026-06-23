package dev.handziur.network.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.handziur.domain.Product;
import dev.handziur.domain.ProductFilterParams;
import dev.handziur.domain.ProductService;
import dev.handziur.model.LoginRequest;
import dev.handziur.model.TokenResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class StoreServerHTTP {

    private final ProductService productService;
    private final HttpServer server;
    private final ObjectMapper mapper;

    public StoreServerHTTP(ProductService productService, int port) throws IOException {
        this.productService = productService;
        this.mapper = new ObjectMapper();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/login", this::handleLogin);
        this.server.createContext("/products", this::handleProducts);
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            LoginRequest req = mapper.readValue(is, LoginRequest.class);
            if ("admin".equals(req.username()) && "admin".equals(req.password())) {
                String token = JwtUtil.generateToken(req.username());
                sendResponse(exchange, 200, mapper.writeValueAsString(new TokenResponse(token)));
            } else {
                sendResponse(exchange, 401, "Unauthorized");
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, "Bad Request");
        }
    }

    private void handleProducts(HttpExchange exchange) throws IOException {
        if (!authenticate(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        try {
            if ("GET".equals(method) && parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                Product p = productService.read(id);
                if (p == null) {
                    sendResponse(exchange, 404, "Not Found");
                } else {
                    sendResponse(exchange, 200, mapper.writeValueAsString(p));
                }
            } else if ("PUT".equals(method) && parts.length == 2) {
                try (InputStream is = exchange.getRequestBody()) {
                    Product p = mapper.readValue(is, Product.class);
                    List<Product> existing = productService.search(new ProductFilterParams(p.name(), null, null, null, null, null), 1, 0);
                    if (!existing.isEmpty()) {
                        sendResponse(exchange, 400, "Product already exists");
                    } else {
                        Product created = productService.create(p);
                        sendResponse(exchange, 201, mapper.writeValueAsString(created));
                    }
                }
            } else if ("POST".equals(method) && parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                try (InputStream is = exchange.getRequestBody()) {
                    Product p = mapper.readValue(is, Product.class);
                    Product existing = productService.read(id);
                    if (existing == null) {
                        sendResponse(exchange, 404, "Not Found");
                    } else {
                        Product toUpdate = new Product(id, p.name(), p.category(), p.quantity(), p.price());
                        productService.update(toUpdate);
                        sendResponse(exchange, 200, mapper.writeValueAsString(toUpdate));
                    }
                }
            } else if ("DELETE".equals(method) && parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                Product existing = productService.read(id);
                if (existing == null) {
                    sendResponse(exchange, 404, "Not Found");
                } else {
                    productService.delete(id);
                    sendResponse(exchange, 204, "");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    private boolean authenticate(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (JwtUtil.verifyToken(token)) {
                return true;
            }
        }
        sendResponse(exchange, 401, "Unauthorized");
        return false;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}

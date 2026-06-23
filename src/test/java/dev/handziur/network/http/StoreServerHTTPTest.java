package dev.handziur.network.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.handziur.domain.Product;
import dev.handziur.domain.ProductService;
import dev.handziur.model.LoginRequest;
import dev.handziur.model.TokenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerHTTPTest {

    private ProductService productService;
    private StoreServerHTTP server;
    private File tempDb;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String token;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        tempDb = File.createTempFile("test_http_warehouse", ".db");
        productService = new ProductService("jdbc:sqlite:" + tempDb.getAbsolutePath());
        server = new StoreServerHTTP(productService, 0);
        server.start();
        port = server.getPort();

        LoginRequest loginReq = new LoginRequest("admin", "admin");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(loginReq)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        TokenResponse tokenResp = mapper.readValue(response.body(), TokenResponse.class);
        token = tokenResp.token();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    void testGetProductNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/999"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void testCrudProductFlow() throws Exception {
        Product newProduct = new Product(0, "TestItem", "TestCategory", 10, 99.9);
        HttpRequest putReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products"))
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(newProduct)))
                .build();

        HttpResponse<String> putResp = client.send(putReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, putResp.statusCode());
        Product created = mapper.readValue(putResp.body(), Product.class);
        assertTrue(created.id() > 0);

        HttpRequest putDuplicateReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products"))
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(newProduct)))
                .build();
        HttpResponse<String> putDuplicateResp = client.send(putDuplicateReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, putDuplicateResp.statusCode());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/" + created.id()))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResp.statusCode());

        Product updatedProduct = new Product(created.id(), "TestItem", "NewCategory", 20, 150.0);
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/" + created.id()))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(updatedProduct)))
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, postResp.statusCode());

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/" + created.id()))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        HttpResponse<String> deleteResp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, deleteResp.statusCode());

        HttpResponse<String> verifyDeleteResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, verifyDeleteResp.statusCode());
    }

    @Test
    void testUnauthorized() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/1"))
                .header("Authorization", "Bearer invalid-token")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }
}

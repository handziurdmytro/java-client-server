package dev.handziur.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {
    private ProductService service;
    private File tempDb;

    @BeforeEach
    void setUp() throws Exception {
        tempDb = File.createTempFile("test_product_service", ".db");
        service = new ProductService("jdbc:sqlite:" + tempDb.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    void testCreateAndReadProduct() {
        Product p = new Product(0, "Mezzanine", "Trip-Hop", 100, 25.5);
        Product created = service.create(p);

        assertTrue(created.id() > 0);

        Product fetched = service.read(created.id());
        assertNotNull(fetched);
        assertEquals("Mezzanine", fetched.name());
        assertEquals(100, fetched.quantity());
    }

    @Test
    void testUpdateProduct() {
        Product created = service.create(new Product(0, "Dummy", "Trip-Hop", 50, 15.0));

        Product updated = new Product(created.id(), "Dummy Remastered", "Trip-Hop", 40, 20.0);
        service.update(updated);

        Product fetched = service.read(created.id());
        assertEquals("Dummy Remastered", fetched.name());
        assertEquals(40, fetched.quantity());
        assertEquals(20.0, fetched.price());
    }

    @Test
    void testDeleteProduct() {
        Product created = service.create(new Product(0, "Treasure", "Shoegaze", 20, 10.0));
        service.delete(created.id());
        Product fetched = service.read(created.id());
        assertNull(fetched);
    }

    @Test
    void testDynamicSearchWithPagination() {
        service.create(new Product(0, "Mezzanine", "Trip-Hop", 10, 150.0));
        service.create(new Product(0, "Dummy", "Trip-Hop", 50, 25.0));
        service.create(new Product(0, "Blue Lines", "Trip-Hop", 30, 45.0));
        service.create(new Product(0, "Heaven or Las Vegas", "Shoegaze", 5, 300.0));

        ProductFilterParams catFilter = new ProductFilterParams(null, "Trip-Hop", null, null, null, null);
        List<Product> tripHopAlbums = service.search(catFilter, 10, 0);
        assertEquals(3, tripHopAlbums.size());

        ProductFilterParams priceFilter = new ProductFilterParams(null, null, null, null, 40.0, null);
        List<Product> expensiveAlbums = service.search(priceFilter, 10, 0);
        assertEquals(3, expensiveAlbums.size());

        ProductFilterParams comboFilter = new ProductFilterParams(null, "Trip-Hop", null, null, null, 100.0);
        List<Product> cheapTripHop = service.search(comboFilter, 10, 0);
        assertEquals(2, cheapTripHop.size());

        List<Product> paged = service.search(comboFilter, 1, 1);
        assertEquals(1, paged.size());
    }
}

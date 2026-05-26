package dev.handziur.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentWarehouseTest {

    private ConcurrentWarehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new ConcurrentWarehouse();
    }

    @Test
    void addAndGetItemQty() {
        warehouse.addItemQty("Mezzanine", 150000);
        warehouse.addItemQty("Mezzanine", 50000);
        assertEquals(200000, warehouse.getItemQty("Mezzanine"));
    }

    @Test
    void getNonExistentItemQty() {
        assertEquals(0, warehouse.getItemQty("Dummy"));
    }

    @Test
    void removeItemQtySuccess() {
        warehouse.addItemQty("Treasure", 250000);
        assertTrue(warehouse.removeItemQty("Treasure", 150000));
        assertEquals(100000, warehouse.getItemQty("Treasure"));
    }

    @Test
    void removeItemQtyFailNotEnough() {
        warehouse.addItemQty("Third", 50000);
        assertFalse(warehouse.removeItemQty("Third", 100000));
        assertEquals(50000, warehouse.getItemQty("Third"));
    }

    @Test
    void removeItemQtyFailNotFound() {
        assertFalse(warehouse.removeItemQty("Protection", 10000));
    }

    @Test
    void createGroup() {
        assertTrue(warehouse.createGroup("TripHop"));
        assertFalse(warehouse.createGroup("TripHop"));
    }

    @Test
    void addGroupName() {
        warehouse.createGroup("Shoegaze");
        assertTrue(warehouse.addGroupName("Shoegaze", "Heaven or Las Vegas"));
    }

    @Test
    void addGroupNameFailNotFound() {
        assertFalse(warehouse.addGroupName("Indie", "The Lonesome Crowded West"));
    }

    @Test
    void setItemPrice() {
        warehouse.setItemPrice("Blue Lines", 99999.99);
        assertDoesNotThrow(() -> warehouse.setItemPrice("Blue Lines", 88888.88));
    }

    @Test
    void concurrentAddItemQty() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> warehouse.addItemQty("Heligoland", 2500));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(2500000, warehouse.getItemQty("Heligoland"));
    }

    @Test
    void concurrentRemoveItemQty() throws InterruptedException {
        warehouse.addItemQty("Blue Bell Knoll", 1000000);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> warehouse.removeItemQty("Blue Bell Knoll", 1000));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(0, warehouse.getItemQty("Blue Bell Knoll"));
    }
}
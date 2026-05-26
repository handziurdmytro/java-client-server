package dev.handziur.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentWarehouse {
    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> prices = new ConcurrentHashMap<>();

    public int getItemQty(String item) {
        AtomicInteger count = inventory.get(item);
        return count != null ? count.get() : 0;
    }

    public void addItemQty(String item, int amount) {
        inventory.computeIfAbsent(item, k -> new AtomicInteger(0))
                .addAndGet(amount);
    }

    public boolean removeItemQty(String product, int amount) {
        AtomicInteger current = inventory.get(product);
        if (current == null) return false;

        while (true) {
            int currentVal = current.get();
            if (currentVal < amount) return false;

            if (current.compareAndSet(currentVal, currentVal - amount)) {
                return true;
            }
        }
    }

    public boolean createGroup(String groupName) {
        return groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet()) == null;
    }

    public boolean addGroupName(String groupName, String itemName) {
        Set<String> items = groups.get(groupName);
        if (items == null) return false;

        return items.add(itemName);
    }

    public void setItemPrice(String item, double price) {
        prices.put(item, price);
    }
}

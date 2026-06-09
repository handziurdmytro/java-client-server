package dev.handziur.domain;

public record Product(
        int id,
        String name,
        String category,
        int quantity,
        double price
) {
    public Product withId(int newId) {
        return new Product(newId, name, category, quantity, price);
    }
}

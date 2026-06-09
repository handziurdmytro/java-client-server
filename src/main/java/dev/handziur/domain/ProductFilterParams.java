package dev.handziur.domain;

public record ProductFilterParams(
        String name,
        String category,
        Integer minQty,
        Integer maxQty,
        Double minPrice,
        Double maxPrice
) {}

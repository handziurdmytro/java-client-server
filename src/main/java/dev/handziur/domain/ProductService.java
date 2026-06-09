package dev.handziur.domain;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {
    private final String dbUrl;

    public ProductService(String dbUrl) {
        this.dbUrl = dbUrl;
        initDb();
    }

    private void initDb() {
        String sql = """
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT,
                    quantity INTEGER NOT NULL,
                    price REAL NOT NULL
                );""";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public Product create(Product p) {
        String sql = "INSERT INTO products(name, category, quantity, price) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, p.name());
            pstmt.setString(2, p.category());
            pstmt.setInt(3, p.quantity());
            pstmt.setDouble(4, p.price());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return p.withId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return p;
    }

    public Product read(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void update(Product p) {
        String sql = "UPDATE products SET name=?, category=?, quantity=?, price=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.name());
            pstmt.setString(2, p.category());
            pstmt.setInt(3, p.quantity());
            pstmt.setDouble(4, p.price());
            pstmt.setInt(5, p.id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Product> search(ProductFilterParams filter, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filter.name() != null && !filter.name().isBlank()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + filter.name() + "%");
        }
        if (filter.category() != null && !filter.category().isBlank()) {
            sql.append(" AND category = ?");
            params.add(filter.category());
        }
        if (filter.minQty() != null) {
            sql.append(" AND quantity >= ?");
            params.add(filter.minQty());
        }
        if (filter.maxQty() != null) {
            sql.append(" AND quantity <= ?");
            params.add(filter.maxQty());
        }
        if (filter.minPrice() != null) {
            sql.append(" AND price >= ?");
            params.add(filter.minPrice());
        }
        if (filter.maxPrice() != null) {
            sql.append(" AND price <= ?");
            params.add(filter.maxPrice());
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Product> results = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("quantity"),
                rs.getDouble("price")
        );
    }
}

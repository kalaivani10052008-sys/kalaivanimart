package com.kalaivanimart.dao;

import com.kalaivanimart.config.DatabaseConfig;
import com.kalaivanimart.model.CartItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    public List<CartItem> getCartItems(int userId) {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT c.id, c.user_id, c.product_id, c.quantity, p.name, p.image, p.price, p.stock " +
                     "FROM cart_items c JOIN products p ON c.product_id = p.id " +
                     "WHERE c.user_id = ? ORDER BY c.id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("image"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getInt("stock")
                    );
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public boolean addToCart(int userId, int productId, int quantity) {
        String checkSql = "SELECT id, quantity FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            // First check product stock
            try (PreparedStatement pps = conn.prepareStatement("SELECT stock FROM products WHERE id = ?")) {
                pps.setInt(1, productId);
                ResultSet prs = pps.executeQuery();
                if (!prs.next() || prs.getInt("stock") <= 0) {
                    return false; // Out of stock
                }
                int availableStock = prs.getInt("stock");

                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int currentQty = rs.getInt("quantity");
                        int newQty = Math.min(currentQty + quantity, availableStock);
                        try (PreparedStatement ups = conn.prepareStatement("UPDATE cart_items SET quantity = ? WHERE id = ?")) {
                            ups.setInt(1, newQty);
                            ups.setInt(2, rs.getInt("id"));
                            return ups.executeUpdate() > 0;
                        }
                    } else {
                        int newQty = Math.min(quantity, availableStock);
                        try (PreparedStatement ips = conn.prepareStatement("INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?)")) {
                            ips.setInt(1, userId);
                            ips.setInt(2, productId);
                            ips.setInt(3, newQty);
                            return ips.executeUpdate() > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateQuantity(int userId, int productId, int quantity) {
        if (quantity <= 0) {
            return removeFromCart(userId, productId);
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check available stock
            try (PreparedStatement pps = conn.prepareStatement("SELECT stock FROM products WHERE id = ?")) {
                pps.setInt(1, productId);
                ResultSet prs = pps.executeQuery();
                if (!prs.next()) {
                    return false;
                }
                int availableStock = prs.getInt("stock");
                int qtyToSet = Math.min(quantity, availableStock);

                String sql = "UPDATE cart_items SET quantity = ? WHERE user_id = ? AND product_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, qtyToSet);
                    ps.setInt(2, userId);
                    ps.setInt(3, productId);
                    return ps.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeFromCart(int userId, int productId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearCart(int userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

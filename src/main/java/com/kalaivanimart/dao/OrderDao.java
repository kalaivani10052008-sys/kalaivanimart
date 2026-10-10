package com.kalaivanimart.dao;

import com.kalaivanimart.config.DatabaseConfig;
import com.kalaivanimart.model.CartItem;
import com.kalaivanimart.model.Order;
import com.kalaivanimart.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    public synchronized Order createOrder(int userId, String address, String paymentMethod, List<CartItem> items) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Validate stocks and calculate total
            double total = 0.0;
            for (CartItem item : items) {
                try (PreparedStatement checkPs = conn.prepareStatement("SELECT name, price, stock FROM products WHERE id = ? FOR UPDATE")) {
                    checkPs.setInt(1, item.getProductId());
                    ResultSet rs = checkPs.executeQuery();
                    if (!rs.next()) {
                        throw new IllegalStateException("Product not found: " + item.getName());
                    }
                    int currentStock = rs.getInt("stock");
                    if (currentStock < item.getQuantity()) {
                        throw new IllegalStateException("Insufficient stock for product: " + rs.getString("name") + ". Available: " + currentStock);
                    }
                    total += rs.getDouble("price") * item.getQuantity();
                }
            }

            // 2. Insert into orders table
            int orderId = 0;
            String insertOrderSql = "INSERT INTO orders (user_id, total, status, address, payment_method) VALUES (?, ?, 'Pending', ?, ?)";
            try (PreparedStatement orderPs = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderPs.setInt(1, userId);
                orderPs.setDouble(2, total);
                orderPs.setString(3, address);
                orderPs.setString(4, paymentMethod != null ? paymentMethod : "COD");
                orderPs.executeUpdate();

                ResultSet generatedKeys = orderPs.getGeneratedKeys();
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to create order, no ID obtained.");
                }
            }

            // 3. Insert order items & update product stock
            String insertItemSql = "INSERT INTO order_items (order_id, product_id, product_name, product_image, price, quantity) VALUES (?, ?, ?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock = stock - ? WHERE id = ?";

            try (PreparedStatement itemPs = conn.prepareStatement(insertItemSql);
                 PreparedStatement stockPs = conn.prepareStatement(updateStockSql)) {

                for (CartItem item : items) {
                    itemPs.setInt(1, orderId);
                    itemPs.setInt(2, item.getProductId());
                    itemPs.setString(3, item.getName());
                    itemPs.setString(4, item.getImage());
                    itemPs.setDouble(5, item.getPrice());
                    itemPs.setInt(6, item.getQuantity());
                    itemPs.addBatch();

                    stockPs.setInt(1, item.getQuantity());
                    stockPs.setInt(2, item.getProductId());
                    stockPs.addBatch();
                }
                itemPs.executeBatch();
                stockPs.executeBatch();
            }

            // 4. Clear user's cart
            try (PreparedStatement clearPs = conn.prepareStatement("DELETE FROM cart_items WHERE user_id = ?")) {
                clearPs.setInt(1, userId);
                clearPs.executeUpdate();
            }

            // Commit transaction
            conn.commit();

            Order order = new Order();
            order.setId(orderId);
            order.setUserId(userId);
            order.setTotal(total);
            order.setStatus("Pending");
            order.setAddress(address);
            order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
            order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            return order;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    public List<Order> getOrdersByUserId(int userId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.user_id = ? ORDER BY o.id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderItems(order.getId(), conn));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email FROM orders o JOIN users u ON o.user_id = u.id ORDER BY o.id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Order order = mapOrder(rs);
                order.setItems(getOrderItems(order.getId(), conn));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public boolean updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getTotalOrdersCount() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total), 0.0) FROM orders WHERE status != 'Cancelled'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private List<OrderItem> getOrderItems(int orderId, Connection conn) {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem(
                        rs.getInt("id"),
                        rs.getInt("order_id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("product_image"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                    );
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setName(rs.getString("user_name"));
        o.setEmail(rs.getString("user_email"));
        o.setTotal(rs.getDouble("total"));
        o.setStatus(rs.getString("status"));
        o.setAddress(rs.getString("address"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        return o;
    }
}

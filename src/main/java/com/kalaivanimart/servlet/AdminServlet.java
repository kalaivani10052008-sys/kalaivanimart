package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.OrderDao;
import com.kalaivanimart.dao.ProductDao;
import com.kalaivanimart.dao.UserDao;
import com.kalaivanimart.model.Order;
import com.kalaivanimart.model.Product;
import com.kalaivanimart.model.User;
import com.kalaivanimart.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminServlet", urlPatterns = {"/api/admin/*"})
public class AdminServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();
    private final ProductDao productDao = new ProductDao();
    private final OrderDao orderDao = new OrderDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        if ("/stats".equals(pathInfo)) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("users", userDao.getTotalUsersCount());
            stats.put("products", productDao.getTotalProductsCount());
            stats.put("orders", orderDao.getTotalOrdersCount());
            stats.put("revenue", orderDao.getTotalRevenue());
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, stats);
        } else if ("/products".equals(pathInfo)) {
            List<Product> products = productDao.getAllProducts("all", null);
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, products);
        } else if ("/orders".equals(pathInfo)) {
            List<Order> orders = orderDao.getAllOrders();
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, orders);
        } else if ("/users".equals(pathInfo)) {
            List<User> users = userDao.getAllUsers();
            for (User u : users) {
                u.setPassword(null); // Never send password hash to frontend
            }
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, users);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin endpoint not found.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/products".equals(pathInfo)) {
            JsonObject json = JsonUtil.readObject(req, JsonObject.class);
            if (json == null || !json.has("name") || !json.has("price")) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name and price are required.");
                return;
            }

            Product p = new Product();
            p.setName(json.get("name").getAsString().trim());
            p.setDescription(json.has("description") ? json.get("description").getAsString().trim() : "");
            p.setCategory(json.has("category") ? json.get("category").getAsString().trim() : "general");
            p.setPrice(json.get("price").getAsDouble());
            p.setOld_price(json.has("old_price") && !json.get("old_price").isJsonNull() ? json.get("old_price").getAsDouble() : 0.0);
            p.setStock(json.has("stock") && !json.get("stock").isJsonNull() ? json.get("stock").getAsInt() : 0);
            p.setImage(json.has("image") && !json.get("image").isJsonNull() ? json.get("image").getAsString().trim() : "");
            p.setSellerId(1);

            if (productDao.addProduct(p)) {
                JsonUtil.sendSuccess(resp, "Product added successfully.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add product.");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin endpoint not found.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/orders/")) {
            try {
                int orderId = Integer.parseInt(pathInfo.substring("/orders/".length()));
                JsonObject json = JsonUtil.readObject(req, JsonObject.class);
                if (json == null || !json.has("status")) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Status is required.");
                    return;
                }
                String status = json.get("status").getAsString().trim();
                if (orderDao.updateOrderStatus(orderId, status)) {
                    JsonUtil.sendSuccess(resp, "Order status updated to " + status + ".");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update order status.");
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid order ID.");
            }
        } else if (pathInfo != null && pathInfo.startsWith("/products/")) {
            try {
                int prodId = Integer.parseInt(pathInfo.substring("/products/".length()));
                Product p = productDao.getProductById(prodId);
                if (p == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found.");
                    return;
                }
                JsonObject json = JsonUtil.readObject(req, JsonObject.class);
                if (json.has("name")) p.setName(json.get("name").getAsString().trim());
                if (json.has("description")) p.setDescription(json.get("description").getAsString().trim());
                if (json.has("category")) p.setCategory(json.get("category").getAsString().trim());
                if (json.has("price")) p.setPrice(json.get("price").getAsDouble());
                if (json.has("old_price")) p.setOld_price(json.get("old_price").getAsDouble());
                if (json.has("stock")) p.setStock(json.get("stock").getAsInt());
                if (json.has("image")) p.setImage(json.get("image").getAsString().trim());

                if (productDao.updateProduct(p)) {
                    JsonUtil.sendSuccess(resp, "Product updated successfully.");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update product.");
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin endpoint not found.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/products/")) {
            try {
                int prodId = Integer.parseInt(pathInfo.substring("/products/".length()));
                if (productDao.deleteProduct(prodId)) {
                    JsonUtil.sendSuccess(resp, "Product deleted successfully.");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete product.");
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin endpoint not found.");
        }
    }
}

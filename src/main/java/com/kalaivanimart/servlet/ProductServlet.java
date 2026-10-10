package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.ProductDao;
import com.kalaivanimart.model.Product;
import com.kalaivanimart.model.User;
import com.kalaivanimart.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "ProductServlet", urlPatterns = {"/api/products", "/api/products/*"})
public class ProductServlet extends HttpServlet {

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                int id = Integer.parseInt(pathInfo.substring(1));
                Product product = productDao.getProductById(id);
                if (product != null) {
                    JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, product);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found");
                }
                return;
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID");
                return;
            }
        }

        String category = req.getParameter("category");
        String search = req.getParameter("search");
        List<Product> list = productDao.getAllProducts(category, search);
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, list);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || (!"admin".equalsIgnoreCase(user.getRole()) && !"seller".equalsIgnoreCase(user.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only admins and sellers can add products.");
            return;
        }

        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("name") || !json.has("price")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product name and price are required.");
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
        p.setSellerId(user.getId());

        if (p.getName().isEmpty() || p.getPrice() < 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid product name and positive price are required.");
            return;
        }

        if (productDao.addProduct(p)) {
            JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, p);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save product.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || (!"admin".equalsIgnoreCase(user.getRole()) && !"seller".equalsIgnoreCase(user.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID required.");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Product existing = productDao.getProductById(id);
            if (existing == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found.");
                return;
            }

            // If seller, ensure seller owns the product or user is admin
            if ("seller".equalsIgnoreCase(user.getRole()) && existing.getSellerId() != user.getId()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "You can only update your own products.");
                return;
            }

            JsonObject json = JsonUtil.readObject(req, JsonObject.class);
            if (json.has("name")) existing.setName(json.get("name").getAsString().trim());
            if (json.has("description")) existing.setDescription(json.get("description").getAsString().trim());
            if (json.has("category")) existing.setCategory(json.get("category").getAsString().trim());
            if (json.has("price")) existing.setPrice(json.get("price").getAsDouble());
            if (json.has("old_price")) existing.setOld_price(json.get("old_price").getAsDouble());
            if (json.has("stock")) existing.setStock(json.get("stock").getAsInt());
            if (json.has("image")) existing.setImage(json.get("image").getAsString().trim());

            if (productDao.updateProduct(existing)) {
                JsonUtil.sendSuccess(resp, "Product updated successfully.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update product.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || (!"admin".equalsIgnoreCase(user.getRole()) && !"seller".equalsIgnoreCase(user.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID required.");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Product existing = productDao.getProductById(id);
            if (existing == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found.");
                return;
            }

            if ("seller".equalsIgnoreCase(user.getRole()) && existing.getSellerId() != user.getId()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "You can only delete your own products.");
                return;
            }

            if (productDao.deleteProduct(id)) {
                JsonUtil.sendSuccess(resp, "Product deleted successfully.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete product.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
        }
    }
}

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

@WebServlet(name = "SellerServlet", urlPatterns = {"/api/seller/*"})
public class SellerServlet extends HttpServlet {

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || (!"seller".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Seller access required.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if ("/products".equals(pathInfo) || pathInfo == null || pathInfo.isEmpty()) {
            List<Product> list = productDao.getProductsBySeller(user.getId());
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, list);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Seller endpoint not found.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || (!"seller".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Seller access required.");
            return;
        }

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
        p.setSellerId(user.getId());

        if (productDao.addProduct(p)) {
            JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, p);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add seller product.");
        }
    }
}

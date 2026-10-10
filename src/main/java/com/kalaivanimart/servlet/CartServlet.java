package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.CartDao;
import com.kalaivanimart.model.CartItem;
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

@WebServlet(name = "CartServlet", urlPatterns = {"/api/cart", "/api/cart/*"})
public class CartServlet extends HttpServlet {

    private final CartDao cartDao = new CartDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view your shopping cart.");
            return;
        }

        List<CartItem> items = cartDao.getCartItems(user.getId());
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, items);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to add items to your cart.");
            return;
        }

        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("productId")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID is required.");
            return;
        }

        int productId = json.get("productId").getAsInt();
        int quantity = json.has("quantity") ? json.get("quantity").getAsInt() : 1;
        if (quantity <= 0) quantity = 1;

        if (cartDao.addToCart(user.getId(), productId, quantity)) {
            JsonUtil.sendSuccess(resp, "Added to cart successfully.");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Item is out of stock or could not be added.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID required.");
            return;
        }

        try {
            int productId = Integer.parseInt(pathInfo.substring(1));
            JsonObject json = JsonUtil.readObject(req, JsonObject.class);
            if (json == null || !json.has("quantity")) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Quantity is required.");
                return;
            }

            int quantity = json.get("quantity").getAsInt();
            if (cartDao.updateQuantity(user.getId(), productId, quantity)) {
                JsonUtil.sendSuccess(resp, "Cart updated.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unable to update quantity (stock limit reached).");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID required.");
            return;
        }

        try {
            int productId = Integer.parseInt(pathInfo.substring(1));
            if (cartDao.removeFromCart(user.getId(), productId)) {
                JsonUtil.sendSuccess(resp, "Item removed from cart.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Item not found in cart.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
        }
    }
}

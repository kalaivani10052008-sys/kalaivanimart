package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.WishlistDao;
import com.kalaivanimart.model.User;
import com.kalaivanimart.model.WishlistItem;
import com.kalaivanimart.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "WishlistServlet", urlPatterns = {"/api/wishlist", "/api/wishlist/*"})
public class WishlistServlet extends HttpServlet {

    private final WishlistDao wishlistDao = new WishlistDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view wishlist.");
            return;
        }

        List<WishlistItem> list = wishlistDao.getWishlistByUserId(user.getId());
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, list);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to manage wishlist.");
            return;
        }

        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("productId")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID is required.");
            return;
        }

        int productId = json.get("productId").getAsInt();
        if (wishlistDao.addToWishlist(user.getId(), productId)) {
            JsonUtil.sendSuccess(resp, "Added to wishlist.");
        } else {
            JsonUtil.sendSuccess(resp, "Item is already in wishlist.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to manage wishlist.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID required.");
            return;
        }

        try {
            int productId = Integer.parseInt(pathInfo.substring(1));
            if (wishlistDao.removeFromWishlist(user.getId(), productId)) {
                JsonUtil.sendSuccess(resp, "Removed from wishlist.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Item not found in wishlist.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
        }
    }
}

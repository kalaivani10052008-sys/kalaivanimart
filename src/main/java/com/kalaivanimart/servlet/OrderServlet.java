package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.CartDao;
import com.kalaivanimart.dao.OrderDao;
import com.kalaivanimart.model.CartItem;
import com.kalaivanimart.model.Order;
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

@WebServlet(name = "OrderServlet", urlPatterns = {"/api/orders", "/api/orders/*"})
public class OrderServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();
    private final CartDao cartDao = new CartDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view orders.");
            return;
        }

        List<Order> orders = orderDao.getOrdersByUserId(user.getId());
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, orders);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to place an order.");
            return;
        }

        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("address")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Delivery address is required.");
            return;
        }

        String address = json.get("address").getAsString().trim();
        if (address.length() < 10) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please provide a complete delivery address with street, city and pincode.");
            return;
        }

        String paymentMethod = json.has("paymentMethod") ? json.get("paymentMethod").getAsString() : "Cash on Delivery";

        List<CartItem> cartItems = cartDao.getCartItems(user.getId());
        if (cartItems.isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Your shopping cart is empty.");
            return;
        }

        try {
            Order order = orderDao.createOrder(user.getId(), address, paymentMethod, cartItems);
            JsonUtil.sendJson(resp, HttpServletResponse.SC_CREATED, order);
        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to place order: " + e.getMessage());
        }
    }
}

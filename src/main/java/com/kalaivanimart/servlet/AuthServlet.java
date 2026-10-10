package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.UserDao;
import com.kalaivanimart.model.User;
import com.kalaivanimart.util.JsonUtil;
import com.kalaivanimart.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = {
    "/api/login",
    "/api/register",
    "/api/logout",
    "/api/me",
    "/api/profile",
    "/api/admin/login"
})
public class AuthServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/api/me".equals(path)) {
            handleMe(req, resp);
        } else if ("/api/profile".equals(path)) {
            handleGetProfile(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/api/register".equals(path)) {
            handleRegister(req, resp);
        } else if ("/api/login".equals(path)) {
            handleLogin(req, resp, false);
        } else if ("/api/admin/login".equals(path)) {
            handleLogin(req, resp, true);
        } else if ("/api/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/api/profile".equals(path)) {
            handleUpdateProfile(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("loggedIn", true);
            result.put("user", sanitizeUser(user));
        } else {
            result.put("loggedIn", false);
        }
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, result);
    }

    private void handleGetProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view profile");
            return;
        }
        User fresh = userDao.getUserById(user.getId());
        if (fresh == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User profile not found");
            return;
        }
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, sanitizeUser(fresh));
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("name") || !json.has("email") || !json.has("password")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email, and password are required.");
            return;
        }

        String name = json.get("name").getAsString().trim();
        String email = json.get("email").getAsString().trim().toLowerCase();
        String password = json.get("password").getAsString();
        String role = json.has("role") ? json.get("role").getAsString().trim() : "buyer";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please fill in all required fields.");
            return;
        }

        if (password.length() < 6) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters.");
            return;
        }

        if (userDao.getUserByEmail(email) != null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "An account with this email already exists.");
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtil.hashPassword(password));
        user.setRole("seller".equalsIgnoreCase(role) ? "seller" : "buyer");

        if (json.has("phone")) user.setPhone(json.get("phone").getAsString().trim());
        if (json.has("address")) user.setAddress(json.get("address").getAsString().trim());
        if (json.has("city")) user.setCity(json.get("city").getAsString().trim());
        if (json.has("state")) user.setState(json.get("state").getAsString().trim());
        if (json.has("pincode")) user.setPincode(json.get("pincode").getAsString().trim());

        if (userDao.createUser(user)) {
            JsonUtil.sendSuccess(resp, "Account created successfully. You can now login.");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create account. Please try again.");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp, boolean requireAdmin) throws IOException {
        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("email") || !json.has("password")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required.");
            return;
        }

        String email = json.get("email").getAsString().trim().toLowerCase();
        String password = json.get("password").getAsString();

        User user = userDao.getUserByEmail(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.getPassword())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password.");
            return;
        }

        if (requireAdmin && !"admin".equalsIgnoreCase(user.getRole())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied. Administrator account required.");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Welcome back, " + user.getName() + "!");
        result.put("user", sanitizeUser(user));
        JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, result);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendSuccess(resp, "Logged out successfully.");
    }

    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please login first.");
            return;
        }

        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request data.");
            return;
        }

        if (json.has("name")) user.setName(json.get("name").getAsString().trim());
        if (json.has("phone")) user.setPhone(json.get("phone").getAsString().trim());
        if (json.has("address")) user.setAddress(json.get("address").getAsString().trim());
        if (json.has("city")) user.setCity(json.get("city").getAsString().trim());
        if (json.has("state")) user.setState(json.get("state").getAsString().trim());
        if (json.has("pincode")) user.setPincode(json.get("pincode").getAsString().trim());

        if (userDao.updateProfile(user)) {
            session.setAttribute("user", user);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Profile updated successfully.");
            res.put("user", sanitizeUser(user));
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, res);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update profile.");
        }
    }

    private Map<String, Object> sanitizeUser(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("phone", u.getPhone() != null ? u.getPhone() : "");
        map.put("address", u.getAddress() != null ? u.getAddress() : "");
        map.put("city", u.getCity() != null ? u.getCity() : "");
        map.put("state", u.getState() != null ? u.getState() : "");
        map.put("pincode", u.getPincode() != null ? u.getPincode() : "");
        map.put("role", u.getRole());
        return map;
    }
}

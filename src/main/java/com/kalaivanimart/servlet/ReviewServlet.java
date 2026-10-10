package com.kalaivanimart.servlet;

import com.google.gson.JsonObject;
import com.kalaivanimart.dao.ReviewDao;
import com.kalaivanimart.model.Review;
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

@WebServlet(name = "ReviewServlet", urlPatterns = {"/api/reviews", "/api/reviews/*"})
public class ReviewServlet extends HttpServlet {

    private final ReviewDao reviewDao = new ReviewDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String prodIdParam = req.getParameter("productId");
        if (prodIdParam == null || prodIdParam.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID parameter is required.");
            return;
        }

        try {
            int productId = Integer.parseInt(prodIdParam);
            List<Review> list = reviewDao.getReviewsByProductId(productId);
            JsonUtil.sendJson(resp, HttpServletResponse.SC_OK, list);
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to submit a review.");
            return;
        }

        JsonObject json = JsonUtil.readObject(req, JsonObject.class);
        if (json == null || !json.has("productId") || !json.has("rating") || !json.has("comment")) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID, rating (1-5), and comment are required.");
            return;
        }

        int productId = json.get("productId").getAsInt();
        int rating = json.get("rating").getAsInt();
        String comment = json.get("comment").getAsString().trim();

        if (rating < 1 || rating > 5) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Rating must be between 1 and 5.");
            return;
        }

        Review r = new Review();
        r.setProductId(productId);
        r.setUserId(user.getId());
        r.setUserName(user.getName());
        r.setRating(rating);
        r.setComment(comment);

        if (reviewDao.addReview(r)) {
            JsonUtil.sendSuccess(resp, "Review submitted successfully.");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit review.");
        }
    }
}

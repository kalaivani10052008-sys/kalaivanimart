package com.kalaivanimart.model;

import java.sql.Timestamp;

public class Product {
    private int id;
    private String name;
    private String description;
    private String category;
    private double price;
    private double old_price;
    private int stock;
    private String image;
    private int sellerId;
    private String sellerName;
    private double rating;
    private int reviewsCount;
    private Timestamp createdAt;

    public Product() {}

    public Product(int id, String name, String description, String category, double price, double old_price, int stock, String image, int sellerId, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.old_price = old_price;
        this.stock = stock;
        this.image = image;
        this.sellerId = sellerId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getOld_price() { return old_price; }
    public void setOld_price(double old_price) { this.old_price = old_price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

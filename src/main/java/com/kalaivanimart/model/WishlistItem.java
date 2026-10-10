package com.kalaivanimart.model;

public class WishlistItem {
    private int id;
    private int userId;
    private int productId;
    private Product product;

    public WishlistItem() {}

    public WishlistItem(int id, int userId, int productId, Product product) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.product = product;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}

package com.kalaivanimart.model;

public class CartItem {
    private int id;
    private int userId;
    private int productId;
    private String name;
    private String image;
    private double price;
    private int quantity;
    private int stock;

    public CartItem() {}

    public CartItem(int id, int userId, int productId, String name, String image, double price, int quantity, int stock) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.name = name;
        this.image = image;
        this.price = price;
        this.quantity = quantity;
        this.stock = stock;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    // For compatibility with front-end JSON expecting product_id
    public int getProduct_id() { return productId; }
    public void setProduct_id(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}

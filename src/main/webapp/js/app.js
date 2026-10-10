/* ==========================================================================
   KalaivaniMart — Production Front-end Engine
   ========================================================================== */

let currentUser = null;
let selectedCategory = "all";
let searchTimer = null;
let currentProducts = [];
let userWishlistIds = new Set();

// Context path helper (works whether deployed under /kalaivanimart or root /)
const API_BASE = window.location.pathname.startsWith("/kalaivanimart") ? "/kalaivanimart/api" : "/api";

/* =========================
   SESSION & INITIALIZATION
========================= */

async function checkSession() {
    try {
        const response = await fetch(`${API_BASE}/me`);
        const data = await response.json();

        if (data.loggedIn) {
            currentUser = data.user;
            if (currentUser.role === "admin") {
                openAdmin();
            } else {
                openApp();
            }
        } else {
            currentUser = null;
            openApp();
        }
    } catch (err) {
        console.error("Session check error:", err);
        currentUser = null;
        openApp();
    }
}

function showAuth() {
    document.getElementById("auth").style.display = "flex";
    document.getElementById("app").style.display = "none";
    document.getElementById("admin").style.display = "none";
    showLogin();
}

function openApp() {
    document.getElementById("auth").style.display = "none";
    document.getElementById("admin").style.display = "none";
    document.getElementById("app").style.display = "block";

    const logoutBtn = document.getElementById("btnLogout");
    const profileBtn = document.getElementById("btnProfile");
    const sellerBtn = document.getElementById("btnSellerDash");

    if (currentUser) {
        const displayName = currentUser.name.split(" ")[0] || "Profile";
        document.getElementById("userDisplayName").textContent = displayName;
        if (logoutBtn) {
            logoutBtn.style.display = "inline-flex";
            logoutBtn.textContent = "Logout";
            logoutBtn.onclick = logout;
        }
        if (profileBtn) {
            profileBtn.title = "My Profile";
            profileBtn.onclick = showProfile;
        }
        if (sellerBtn) {
            sellerBtn.style.display = currentUser.role === "seller" ? "inline-flex" : "none";
        }
        loadCart();
        loadWishlistIds();
    } else {
        document.getElementById("userDisplayName").textContent = "Sign In";
        if (profileBtn) {
            profileBtn.title = "Sign In / Register";
            profileBtn.onclick = showAuth;
        }
        if (logoutBtn) {
            logoutBtn.style.display = "none";
        }
        if (sellerBtn) {
            sellerBtn.style.display = "none";
        }
        document.getElementById("cartCount").textContent = "0";
        document.getElementById("wishlistCount").textContent = "0";
    }

    loadProducts();
}

function openAdmin() {
    document.getElementById("auth").style.display = "none";
    document.getElementById("app").style.display = "none";
    document.getElementById("admin").style.display = "block";
    loadAdmin();
}

/* =========================
   FORM NAVIGATION
========================= */

function hideForms() {
    document.querySelectorAll(".form").forEach(f => f.classList.remove("active"));
}

function showLogin() {
    hideForms();
    document.getElementById("loginForm").classList.add("active");
}

function showRegister() {
    hideForms();
    document.getElementById("registerForm").classList.add("active");
}

function showAdminLogin() {
    hideForms();
    document.getElementById("adminLoginForm").classList.add("active");
}

/* =========================
   AUTHENTICATION
========================= */

async function login() {
    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;

    if (!email || !password) {
        showToast("Please enter your email and password.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Invalid credentials");
            return;
        }

        currentUser = data.user;
        showToast("Welcome to KalaivaniMart!");
        if (currentUser.role === "admin") {
            openAdmin();
        } else {
            openApp();
        }
    } catch (err) {
        showToast("Server connection error. Please try again.");
    }
}

async function register() {
    const name = document.getElementById("registerName").value.trim();
    const email = document.getElementById("registerEmail").value.trim();
    const phone = document.getElementById("registerPhone").value.trim();
    const password = document.getElementById("registerPassword").value;
    const role = document.getElementById("registerRole").value;

    if (!name || !email || !password) {
        showToast("Please complete all required fields.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, email, phone, password, role })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Registration failed");
            return;
        }

        showToast("Account created successfully. Please sign in.");
        document.getElementById("loginEmail").value = email;
        document.getElementById("loginPassword").value = password;
        showLogin();
    } catch (err) {
        showToast("Server connection error.");
    }
}

async function adminLogin() {
    const email = document.getElementById("adminEmail").value.trim();
    const password = document.getElementById("adminPassword").value;

    if (!email || !password) {
        showToast("Please enter administrative credentials.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/admin/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Access denied.");
            return;
        }

        currentUser = data.user;
        showToast("Administrator session authorized.");
        openAdmin();
    } catch (err) {
        showToast("Server connection error.");
    }
}

async function logout() {
    try {
        await fetch(`${API_BASE}/logout`, { method: "POST" });
    } catch (ignored) {}
    currentUser = null;
    window.location.reload();
}

async function adminLogout() {
    await logout();
}

/* =========================
   PRODUCTS & CATALOG
========================= */

function debounceSearch() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        loadProducts();
    }, 300);
}

function selectCategory(category, buttonEl) {
    selectedCategory = category;
    if (buttonEl) {
        document.querySelectorAll("#categoryNav button").forEach(b => b.classList.remove("active"));
        buttonEl.classList.add("active");
    }

    const titleMap = {
        all: "Featured Products",
        electronics: "Electronics & Smart Devices",
        fashion: "Traditional & Modern Fashion",
        home: "Home & Kitchen Living",
        beauty: "Beauty & Ayurvedic Care",
        grocery: "Daily Essentials & Organic Spices",
        sports: "Sports & Fitness Gear",
        toys: "Traditional Toys & Games"
    };

    const header = document.getElementById("currentCategoryTitle");
    if (header) {
        header.textContent = titleMap[category] || "Products";
    }

    loadProducts();
}

async function loadProducts() {
    const searchInput = document.getElementById("searchInput");
    const search = searchInput ? searchInput.value.trim() : "";

    let url = `${API_BASE}/products?`;
    if (selectedCategory && selectedCategory !== "all") {
        url += `category=${encodeURIComponent(selectedCategory)}&`;
    }
    if (search) {
        url += `search=${encodeURIComponent(search)}`;
    }

    try {
        const response = await fetch(url);
        if (!response.ok) return;
        const products = await response.json();
        currentProducts = products;
        renderProducts(products);
    } catch (err) {
        console.error("Failed to load products:", err);
    }
}

function renderProducts(products) {
    const container = document.getElementById("products");
    const subCount = document.getElementById("productCountSub");

    if (subCount) {
        subCount.textContent = `Showing ${products.length} genuine product${products.length === 1 ? "" : "s"}`;
    }

    if (!products || products.length === 0) {
        container.innerHTML = `
            <div style="grid-column: 1 / -1; background: white; padding: 60px 20px; text-align: center; border-radius: 16px; border: 1px solid var(--border);">
                <div style="font-size: 54px; margin-bottom: 12px;">🛍️</div>
                <h3>No products found</h3>
                <p style="color: var(--text-muted); margin-top: 6px;">Try adjusting your search terms or category filter.</p>
                <button class="primary" style="max-width: 200px; margin: 18px auto 0;" onclick="selectCategory('all')">View All Items</button>
            </div>
        `;
        return;
    }

    container.innerHTML = products.map(product => {
        const isWishlisted = userWishlistIds.has(product.id);
        const discount = product.old_price > product.price 
            ? Math.round(((product.old_price - product.price) / product.old_price) * 100)
            : 0;

        return `
            <article class="product">
                <div class="product-image" onclick="openProductDetails(${product.id})" style="cursor: pointer;">
                    ${discount > 0 ? `<span class="product-badge">${discount}% OFF</span>` : ""}
                    <button class="wishlist-btn" onclick="event.stopPropagation(); toggleWishlist(${product.id}, this)" title="Add to Wishlist">
                        ${isWishlisted ? "❤️" : "🤍"}
                    </button>
                    <img src="${escapeHtml(product.image || 'images/fallback.svg')}" 
                         alt="${escapeHtml(product.name)}" 
                         onerror="this.onerror=null; this.src='images/fallback.svg';">
                </div>
                <div class="product-info">
                    <span class="category-text">${escapeHtml(product.category)}</span>
                    <h3 onclick="openProductDetails(${product.id})" style="cursor: pointer;" title="${escapeHtml(product.name)}">
                        ${escapeHtml(product.name)}
                    </h3>
                    
                    <div class="product-rating">
                        ★★★★★ <span>(${product.reviewsCount || 0} reviews)</span>
                    </div>

                    <div class="product-price">
                        ₹${formatINR(product.price)}
                        ${product.old_price > product.price ? `<span class="old-price">₹${formatINR(product.old_price)}</span>` : ""}
                        ${discount > 0 ? `<span class="discount-tag">${discount}% off</span>` : ""}
                    </div>

                    <div class="stock-status ${product.stock > 0 ? 'stock-in' : 'stock-out'}">
                        ${product.stock > 0 ? `✓ In Stock (${product.stock} units)` : "✕ Currently Out of Stock"}
                    </div>

                    <div class="product-actions">
                        <button class="add" onclick="addToCart(${product.id})" ${product.stock <= 0 ? "disabled" : ""}>
                            Add to Cart
                        </button>
                        <button class="buy" onclick="buyNow(${product.id})" ${product.stock <= 0 ? "disabled" : ""}>
                            Buy Now
                        </button>
                    </div>
                </div>
            </article>
        `;
    }).join("");
}

/* =========================
   PRODUCT DETAILS & REVIEWS
========================= */

async function openProductDetails(productId) {
    try {
        const prodRes = await fetch(`${API_BASE}/products/${productId}`);
        if (!prodRes.ok) return;
        const product = await prodRes.json();

        const revRes = await fetch(`${API_BASE}/reviews?productId=${productId}`);
        const reviews = revRes.ok ? await revRes.json() : [];

        const discount = product.old_price > product.price 
            ? Math.round(((product.old_price - product.price) / product.old_price) * 100)
            : 0;

        document.getElementById("modalProductTitle").textContent = product.name;
        document.getElementById("modalProductContent").innerHTML = `
            <div style="display: grid; grid-template-columns: 1fr 1.2fr; gap: 24px; margin-bottom: 24px;">
                <div style="border-radius: var(--radius-md); overflow: hidden; background: #f8fafc; border: 1px solid var(--border);">
                    <img src="${escapeHtml(product.image || 'images/fallback.svg')}" 
                         alt="${escapeHtml(product.name)}" 
                         style="width: 100%; height: 280px; object-fit: cover;" 
                         onerror="this.onerror=null; this.src='images/fallback.svg';">
                </div>
                <div>
                    <span class="category-text">${escapeHtml(product.category)}</span>
                    <h3 style="font-size: 20px; font-weight: 700; margin: 4px 0 10px;">${escapeHtml(product.name)}</h3>
                    
                    <div class="product-price" style="font-size: 24px;">
                        ₹${formatINR(product.price)}
                        ${product.old_price > product.price ? `<span class="old-price">₹${formatINR(product.old_price)}</span>` : ""}
                        ${discount > 0 ? `<span class="discount-tag">${discount}% off</span>` : ""}
                    </div>

                    <p style="color: var(--text-muted); font-size: 14px; margin: 12px 0;">
                        ${escapeHtml(product.description || 'Authentic quality product verified by KalaivaniMart.')}
                    </p>

                    <div class="stock-status ${product.stock > 0 ? 'stock-in' : 'stock-out'}" style="margin-bottom: 16px;">
                        ${product.stock > 0 ? `✓ Ready to Dispatch (${product.stock} items in stock)` : "✕ Currently Out of Stock"}
                    </div>

                    <div style="display: flex; gap: 10px;">
                        <button class="primary" onclick="addToCart(${product.id}); closeModal('productModal')" ${product.stock <= 0 ? "disabled" : ""}>
                            Add to Cart
                        </button>
                        <button class="primary" style="background: var(--secondary);" onclick="buyNow(${product.id}); closeModal('productModal')" ${product.stock <= 0 ? "disabled" : ""}>
                            Buy Now
                        </button>
                    </div>
                </div>
            </div>

            <!-- REVIEWS SECTION -->
            <div style="border-top: 1px solid var(--border); padding-top: 20px;">
                <h3 style="font-size: 16px; font-weight: 700; margin-bottom: 14px;">Customer Reviews &amp; Ratings</h3>
                
                <!-- ADD REVIEW FORM -->
                <div style="background: #f8fafc; padding: 14px; border-radius: var(--radius-sm); border: 1px solid var(--border); margin-bottom: 20px;">
                    <h4 style="font-size: 14px; font-weight: 600; margin-bottom: 8px;">Write a Review</h4>
                    <div style="display: flex; gap: 12px; margin-bottom: 10px; align-items: center;">
                        <label style="font-size: 13px; font-weight: 600;">Rating:</label>
                        <select id="newReviewRating" style="padding: 6px 10px; border-radius: 4px; border: 1px solid var(--border);">
                            <option value="5">★★★★★ (5 - Excellent)</option>
                            <option value="4">★★★★☆ (4 - Very Good)</option>
                            <option value="3">★★★☆☆ (3 - Good)</option>
                            <option value="2">★★☆☆☆ (2 - Fair)</option>
                            <option value="1">★☆☆☆☆ (1 - Poor)</option>
                        </select>
                    </div>
                    <textarea id="newReviewComment" rows="2" placeholder="Share your experience with this product..." style="width: 100%; padding: 8px; border-radius: 4px; border: 1px solid var(--border); font-size: 13px;"></textarea>
                    <button class="primary" style="max-width: 160px; padding: 8px 14px; font-size: 13px; margin-top: 8px;" onclick="submitReview(${product.id})">
                        Submit Review
                    </button>
                </div>

                <!-- REVIEWS LIST -->
                <div id="productReviewsContainer">
                    ${reviews.length === 0 ? '<p style="color: var(--text-muted); font-size: 13px;">No customer reviews yet. Be the first to review this product!</p>' : reviews.map(r => `
                        <div style="border-bottom: 1px solid var(--border); padding: 10px 0;">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong>${escapeHtml(r.userName || 'Verified Buyer')}</strong>
                                <span style="color: #eab308; font-size: 13px;">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</span>
                            </div>
                            <p style="font-size: 13px; color: var(--dark-muted); margin-top: 4px;">${escapeHtml(r.comment)}</p>
                        </div>
                    `).join("")}
                </div>
            </div>
        `;

        document.getElementById("productModal").classList.add("show");
    } catch (err) {
        console.error("Error opening product details:", err);
    }
}

async function submitReview(productId) {
    const rating = document.getElementById("newReviewRating").value;
    const comment = document.getElementById("newReviewComment").value.trim();

    if (!currentUser) {
        showToast("Please sign in to submit a product review.");
        setTimeout(() => showAuth(), 1200);
        return;
    }

    if (!comment) {
        showToast("Please enter a comment for your review.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/reviews`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ productId, rating: parseInt(rating), comment })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Failed to submit review.");
            return;
        }

        showToast("Review submitted successfully!");
        openProductDetails(productId);
    } catch (err) {
        showToast("Error submitting review.");
    }
}

/* =========================
   CART FUNCTIONALITY
========================= */

async function addToCart(productId, quantity = 1) {
    if (!currentUser) {
        showToast("Please sign in or create an account to add items to your cart.");
        setTimeout(() => showAuth(), 1200);
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/cart`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ productId, quantity })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Failed to add to cart.");
            return;
        }

        showToast("Added to your cart 🛒");
        loadCart();
    } catch (err) {
        showToast("Server connection error.");
    }
}

async function buyNow(productId) {
    if (!currentUser) {
        showToast("Please sign in to proceed with checkout.");
        setTimeout(() => showAuth(), 1200);
        return;
    }
    await addToCart(productId);
    openCheckout();
}

async function loadCart() {
    try {
        const response = await fetch(`${API_BASE}/cart`);
        if (!response.ok) return;
        const items = await response.json();

        const count = items.reduce((sum, item) => sum + item.quantity, 0);
        document.getElementById("cartCount").textContent = count;

        const container = document.getElementById("cartItems");
        if (items.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 60px 10px; color: var(--text-muted);">
                    <div style="font-size: 50px; margin-bottom: 10px;">🛒</div>
                    <h3 style="font-size: 16px; font-weight: 700; color: var(--dark);">Your cart is empty</h3>
                    <p style="font-size: 13px; margin-top: 4px;">Explore our categories and add products.</p>
                </div>
            `;
            document.getElementById("cartTotal").textContent = "₹0";
            return;
        }

        let total = 0;
        container.innerHTML = items.map(item => {
            total += item.price * item.quantity;
            return `
                <div class="cart-item">
                    <img src="${escapeHtml(item.image || 'images/fallback.svg')}" 
                         alt="${escapeHtml(item.name)}"
                         onerror="this.onerror=null; this.src='images/fallback.svg';">
                    <div class="cart-info">
                        <strong>${escapeHtml(item.name)}</strong>
                        <div style="color: var(--primary); font-weight: 700;">₹${formatINR(item.price)}</div>
                        <div class="qty">
                            <button onclick="changeQuantity(${item.productId || item.product_id}, ${item.quantity - 1})">-</button>
                            <span>${item.quantity}</span>
                            <button onclick="changeQuantity(${item.productId || item.product_id}, ${item.quantity + 1})">+</button>
                        </div>
                        <button class="remove" onclick="removeCartItem(${item.productId || item.product_id})">Remove</button>
                    </div>
                </div>
            `;
        }).join("");

        document.getElementById("cartTotal").textContent = `₹${formatINR(total)}`;
        const checkoutTotal = document.getElementById("checkoutOrderTotal");
        if (checkoutTotal) checkoutTotal.textContent = `₹${formatINR(total)}`;

    } catch (err) {
        console.error("Cart error:", err);
    }
}

async function changeQuantity(productId, quantity) {
    try {
        const response = await fetch(`${API_BASE}/cart/${productId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ quantity })
        });
        const data = await response.json();
        if (!response.ok) {
            showToast(data.message || "Cannot update quantity");
            return;
        }
        loadCart();
    } catch (err) {
        showToast("Error updating cart.");
    }
}

async function removeCartItem(productId) {
    try {
        await fetch(`${API_BASE}/cart/${productId}`, { method: "DELETE" });
        loadCart();
    } catch (err) {
        showToast("Error removing item.");
    }
}

function openCart() {
    if (!currentUser) {
        showToast("Please sign in to view your shopping cart.");
        setTimeout(() => showAuth(), 1200);
        return;
    }
    document.getElementById("cartPanel").classList.add("open");
    document.getElementById("overlay").classList.add("show");
    loadCart();
}

function closePanels() {
    document.getElementById("cartPanel").classList.remove("open");
    document.getElementById("overlay").classList.remove("show");
}

/* =========================
   CHECKOUT & ORDERS
========================= */

function openCheckout() {
    closePanels();
    if (!currentUser) {
        showToast("Please sign in to proceed with checkout.");
        setTimeout(() => showAuth(), 1200);
        return;
    }
    const addrField = document.getElementById("address");
    if (!addrField.value.trim() && currentUser.address) {
        addrField.value = `${currentUser.address}, ${currentUser.city || ''}, ${currentUser.state || ''} - ${currentUser.pincode || ''}`.trim();
    }
    document.getElementById("checkoutModal").classList.add("show");
}

async function placeOrder() {
    const address = document.getElementById("address").value.trim();
    const paymentMethod = document.getElementById("paymentMethodSelect").value;

    if (address.length < 10) {
        showToast("Please provide a complete delivery address (street, city, pincode).");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ address, paymentMethod })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Failed to place order.");
            return;
        }

        closeModal("checkoutModal");
        document.getElementById("address").value = "";
        loadCart();
        loadProducts();

        showToast(`Order #${data.id} confirmed! Thank you for shopping with KalaivaniMart 🎉`);
        setTimeout(() => showOrders(), 800);
    } catch (err) {
        showToast("Server connection error while placing order.");
    }
}

async function showOrders() {
    if (!currentUser) {
        showToast("Please sign in to view your order history.");
        setTimeout(() => showAuth(), 1200);
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders`);
        if (!response.ok) return;
        const orders = await response.json();

        const container = document.getElementById("ordersList");
        if (orders.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 40px; color: var(--text-muted);">
                    <div style="font-size: 40px; margin-bottom: 8px;">📦</div>
                    <h3>No orders placed yet</h3>
                    <p style="font-size: 13px;">Your confirmed orders and live tracking will appear here.</p>
                </div>
            `;
        } else {
            container.innerHTML = orders.map(order => `
                <div style="border: 1px solid var(--border); padding: 18px; border-radius: var(--radius-sm); margin-bottom: 14px; background: #ffffff;">
                    <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 10px; margin-bottom: 10px;">
                        <div>
                            <strong style="color: var(--dark); font-size: 15px;">Order #${order.id}</strong>
                            <div style="font-size: 12px; color: var(--text-muted);">${order.created_at || ''}</div>
                        </div>
                        <div style="text-align: right;">
                            <span style="display: inline-block; padding: 4px 10px; border-radius: var(--radius-full); font-size: 12px; font-weight: 700; background: ${getStatusBg(order.status)}; color: ${getStatusColor(order.status)};">
                                ${escapeHtml(order.status)}
                            </span>
                        </div>
                    </div>

                    ${order.items && order.items.length > 0 ? `
                        <div style="margin-bottom: 12px; display: flex; flex-direction: column; gap: 8px;">
                            ${order.items.map(it => `
                                <div style="display: flex; align-items: center; gap: 10px; font-size: 13px; background: #f8fafc; padding: 6px 10px; border-radius: 6px;">
                                    <img src="${escapeHtml(it.productImage || 'images/fallback.svg')}" 
                                         alt="${escapeHtml(it.productName)}" 
                                         style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px;"
                                         onerror="this.onerror=null; this.src='images/fallback.svg';">
                                    <div style="flex: 1;">
                                        <strong>${escapeHtml(it.productName)}</strong>
                                        <div style="color: var(--text-muted); font-size: 12px;">Qty: ${it.quantity} &times; ₹${formatINR(it.price)}</div>
                                    </div>
                                    <div style="font-weight: 700; color: var(--primary);">₹${formatINR(it.price * it.quantity)}</div>
                                </div>
                            `).join("")}
                        </div>
                    ` : ""}

                    <div style="font-size: 14px; margin-bottom: 8px;">
                        <strong>Total Amount: </strong> <span style="color: var(--primary); font-weight: 700;">₹${formatINR(order.total)}</span>
                        <span style="font-size: 12px; color: var(--text-muted); margin-left: 8px;">(${escapeHtml(order.paymentMethod || 'COD')})</span>
                    </div>

                    <div style="font-size: 13px; color: var(--dark-muted); background: #f8fafc; padding: 8px 12px; border-radius: 6px;">
                        <strong>Delivery Address: </strong> ${escapeHtml(order.address)}
                    </div>
                </div>
            `).join("");
        }

        document.getElementById("ordersModal").classList.add("show");
    } catch (err) {
        showToast("Error retrieving order history.");
    }
}

function getStatusBg(status) {
    if (status === "Delivered") return "#dcfce7";
    if (status === "Shipped") return "#e0f2fe";
    if (status === "Confirmed") return "#fef3c7";
    if (status === "Cancelled") return "#fee2e2";
    return "#f1f5f9";
}

function getStatusColor(status) {
    if (status === "Delivered") return "#15803d";
    if (status === "Shipped") return "#0369a1";
    if (status === "Confirmed") return "#b45309";
    if (status === "Cancelled") return "#dc2626";
    return "#475569";
}

/* =========================
   WISHLIST
========================= */

async function loadWishlistIds() {
    try {
        const response = await fetch(`${API_BASE}/wishlist`);
        if (!response.ok) return;
        const list = await response.json();
        userWishlistIds = new Set(list.map(w => w.productId));
        document.getElementById("wishlistCount").textContent = userWishlistIds.size;
    } catch (ignored) {}
}

async function toggleWishlist(productId, btnEl) {
    if (!currentUser) {
        showToast("Please sign in to save items to your wishlist.");
        setTimeout(() => showAuth(), 1200);
        return;
    }
    const isPresent = userWishlistIds.has(productId);
    try {
        if (isPresent) {
            await fetch(`${API_BASE}/wishlist/${productId}`, { method: "DELETE" });
            userWishlistIds.delete(productId);
            if (btnEl) btnEl.textContent = "🤍";
            showToast("Removed from Wishlist");
        } else {
            await fetch(`${API_BASE}/wishlist`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ productId })
            });
            userWishlistIds.add(productId);
            if (btnEl) btnEl.textContent = "❤️";
            showToast("Saved to Wishlist ❤️");
        }
        document.getElementById("wishlistCount").textContent = userWishlistIds.size;
    } catch (err) {
        showToast("Error updating wishlist.");
    }
}

async function showWishlist() {
    if (!currentUser) {
        showToast("Please sign in to view your wishlist.");
        setTimeout(() => showAuth(), 1200);
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/wishlist`);
        if (!response.ok) return;
        const list = await response.json();

        const container = document.getElementById("wishlistItemsList");
        if (list.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 40px; color: var(--text-muted);">
                    <div style="font-size: 40px; margin-bottom: 8px;">❤️</div>
                    <h3>Your Wishlist is Empty</h3>
                    <p style="font-size: 13px;">Save items you love by tapping the heart icon on any product.</p>
                </div>
            `;
        } else {
            container.innerHTML = list.map(item => {
                const prod = item.product || {};
                return `
                    <div style="display: flex; gap: 14px; padding: 12px 0; border-bottom: 1px solid var(--border); align-items: center;">
                        <img src="${escapeHtml(prod.image || 'images/fallback.svg')}" 
                             alt="${escapeHtml(prod.name || '')}" 
                             style="width: 60px; height: 60px; object-fit: cover; border-radius: var(--radius-sm);"
                             onerror="this.onerror=null; this.src='images/fallback.svg';">
                        <div style="flex: 1;">
                            <strong style="font-size: 14px;">${escapeHtml(prod.name || '')}</strong>
                            <div style="color: var(--primary); font-weight: 700;">₹${formatINR(prod.price || 0)}</div>
                        </div>
                        <button class="primary" style="width: auto; padding: 8px 14px; font-size: 12px;" onclick="addToCart(${item.productId}); toggleWishlist(${item.productId}); closeModal('wishlistModal')">
                            Move to Cart
                        </button>
                    </div>
                `;
            }).join("");
        }

        document.getElementById("wishlistModal").classList.add("show");
    } catch (err) {
        showToast("Error loading wishlist.");
    }
}

/* =========================
   USER PROFILE
========================= */

async function showProfile() {
    if (!currentUser) {
        showAuth();
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/profile`);
        if (!response.ok) return;
        const u = await response.json();

        document.getElementById("profileName").value = u.name || "";
        document.getElementById("profileEmail").value = u.email || "";
        document.getElementById("profilePhone").value = u.phone || "";
        document.getElementById("profileAddress").value = u.address || "";
        document.getElementById("profileCity").value = u.city || "";
        document.getElementById("profileState").value = u.state || "";
        document.getElementById("profilePincode").value = u.pincode || "";

        document.getElementById("profileModal").classList.add("show");
    } catch (err) {
        showToast("Failed to load profile.");
    }
}

async function saveProfile() {
    const name = document.getElementById("profileName").value.trim();
    const phone = document.getElementById("profilePhone").value.trim();
    const address = document.getElementById("profileAddress").value.trim();
    const city = document.getElementById("profileCity").value.trim();
    const state = document.getElementById("profileState").value.trim();
    const pincode = document.getElementById("profilePincode").value.trim();

    try {
        const response = await fetch(`${API_BASE}/profile`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, phone, address, city, state, pincode })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Failed to update profile.");
            return;
        }

        currentUser = data.user;
        const displayName = currentUser.name.split(" ")[0] || "Profile";
        document.getElementById("userDisplayName").textContent = displayName;
        closeModal("profileModal");
        showToast("Profile details updated successfully.");
    } catch (err) {
        showToast("Error updating profile.");
    }
}

/* =========================
   ADMIN DASHBOARD
========================= */

async function loadAdmin() {
    await loadAdminStats();
    await loadAdminProducts();
    await loadAdminOrders();
    await loadAdminUsers();
}

async function loadAdminStats() {
    try {
        const response = await fetch(`${API_BASE}/admin/stats`);
        if (!response.ok) return;
        const stats = await response.json();

        document.getElementById("statUsers").textContent = stats.users || 0;
        document.getElementById("statProducts").textContent = stats.products || 0;
        document.getElementById("statOrders").textContent = stats.orders || 0;
        document.getElementById("statRevenue").textContent = `₹${formatINR(stats.revenue || 0)}`;
    } catch (err) {
        console.error("Error loading admin stats:", err);
    }
}

async function loadAdminProducts() {
    try {
        const response = await fetch(`${API_BASE}/admin/products`);
        if (!response.ok) return;
        const products = await response.json();

        const container = document.getElementById("adminProducts");
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Product Name</th>
                        <th>Category</th>
                        <th>Price (₹)</th>
                        <th>Stock</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${products.map(p => `
                        <tr>
                            <td>#${p.id}</td>
                            <td><strong>${escapeHtml(p.name)}</strong></td>
                            <td><span style="text-transform: capitalize;">${escapeHtml(p.category)}</span></td>
                            <td>₹${formatINR(p.price)}</td>
                            <td>${p.stock}</td>
                            <td>
                                <button class="small-button delete-button" onclick="deleteProduct(${p.id})">Delete</button>
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;
    } catch (err) {
        console.error("Error loading admin products:", err);
    }
}

async function addProduct() {
    const product = {
        name: document.getElementById("productName").value.trim(),
        description: document.getElementById("productDescription").value.trim(),
        category: document.getElementById("productCategory").value,
        price: Number(document.getElementById("productPrice").value),
        old_price: Number(document.getElementById("productOldPrice").value),
        stock: Number(document.getElementById("productStock").value),
        image: document.getElementById("productImage").value.trim()
    };

    if (!product.name || !product.price || product.price < 0) {
        showToast("Please provide product name and valid positive price.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/admin/products`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(product)
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Failed to add product.");
            return;
        }

        showToast("Product successfully added to KalaivaniMart catalog!");
        document.getElementById("productName").value = "";
        document.getElementById("productDescription").value = "";
        document.getElementById("productPrice").value = "";
        document.getElementById("productOldPrice").value = "";
        document.getElementById("productStock").value = "";
        document.getElementById("productImage").value = "";
        loadAdmin();
    } catch (err) {
        showToast("Error adding product.");
    }
}

async function deleteProduct(id) {
    if (!confirm("Are you sure you want to remove this product from the catalog?")) return;

    try {
        const response = await fetch(`${API_BASE}/admin/products/${id}`, { method: "DELETE" });
        if (response.ok) {
            showToast("Product deleted.");
            loadAdmin();
        } else {
            showToast("Failed to delete product.");
        }
    } catch (err) {
        showToast("Error deleting product.");
    }
}

async function loadAdminOrders() {
    try {
        const response = await fetch(`${API_BASE}/admin/orders`);
        if (!response.ok) return;
        const orders = await response.json();

        const container = document.getElementById("adminOrders");
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Total</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${orders.map(o => `
                        <tr>
                            <td>#${o.id}</td>
                            <td>${escapeHtml(o.name)}<br><small style="color: var(--text-muted);">${escapeHtml(o.email)}</small></td>
                            <td><strong>₹${formatINR(o.total)}</strong></td>
                            <td><span style="font-weight: 700; color: ${getStatusColor(o.status)}">${escapeHtml(o.status)}</span></td>
                            <td>
                                <select style="padding: 4px 8px; border-radius: 4px; border: 1px solid var(--border);" onchange="changeOrderStatus(${o.id}, this.value)">
                                    <option ${o.status === "Pending" ? "selected" : ""}>Pending</option>
                                    <option ${o.status === "Confirmed" ? "selected" : ""}>Confirmed</option>
                                    <option ${o.status === "Shipped" ? "selected" : ""}>Shipped</option>
                                    <option ${o.status === "Delivered" ? "selected" : ""}>Delivered</option>
                                    <option ${o.status === "Cancelled" ? "selected" : ""}>Cancelled</option>
                                </select>
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;
    } catch (err) {
        console.error("Error loading admin orders:", err);
    }
}

async function changeOrderStatus(id, status) {
    try {
        const response = await fetch(`${API_BASE}/admin/orders/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status })
        });
        if (response.ok) {
            showToast(`Order #${id} marked as ${status}.`);
            loadAdminStats();
        } else {
            showToast("Failed to update status.");
        }
    } catch (err) {
        showToast("Error updating order status.");
    }
}

async function loadAdminUsers() {
    try {
        const response = await fetch(`${API_BASE}/admin/users`);
        if (!response.ok) return;
        const users = await response.json();

        const container = document.getElementById("adminUsers");
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Full Name</th>
                        <th>Email Address</th>
                        <th>Role</th>
                        <th>City / State</th>
                    </tr>
                </thead>
                <tbody>
                    ${users.map(u => `
                        <tr>
                            <td>#${u.id}</td>
                            <td><strong>${escapeHtml(u.name)}</strong></td>
                            <td>${escapeHtml(u.email)}</td>
                            <td><span style="display: inline-block; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; background: #f1f5f9; text-transform: uppercase;">${escapeHtml(u.role)}</span></td>
                            <td>${escapeHtml(u.city || '-')}, ${escapeHtml(u.state || '-')}</td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;
    } catch (err) {
        console.error("Error loading admin users:", err);
    }
}

/* =========================
   SELLER DASHBOARD
========================= */

function openSellerPanel() {
    document.getElementById("sellerModal").classList.add("show");
    loadSellerProducts();
}

async function loadSellerProducts() {
    const container = document.getElementById("sellerProductsList");
    try {
        const response = await fetch(`${API_BASE}/seller/products`);
        if (!response.ok) {
            container.innerHTML = '<p style="color:var(--danger);padding:12px;">Unable to load your products. Please ensure you are logged in as a seller.</p>';
            return;
        }
        const products = await response.json();

        if (!products || products.length === 0) {
            container.innerHTML = '<p style="color:var(--text-muted);padding:14px;">You have not listed any products yet. Use the form above to add your first product.</p>';
            return;
        }

        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Product Name</th>
                        <th>Category</th>
                        <th>Price (₹)</th>
                        <th>Stock</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${products.map(p => `
                        <tr>
                            <td>#${p.id}</td>
                            <td><strong>${escapeHtml(p.name)}</strong></td>
                            <td><span style="text-transform:capitalize;">${escapeHtml(p.category)}</span></td>
                            <td>₹${formatINR(p.price)}</td>
                            <td>${p.stock}</td>
                            <td>
                                <button class="small-button delete-button" onclick="sellerDeleteProduct(${p.id})">Remove</button>
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;
    } catch (err) {
        container.innerHTML = '<p style="color:var(--danger);padding:12px;">Error loading products.</p>';
    }
}

async function sellerSaveProduct() {
    const name = document.getElementById("sellerProductName").value.trim();
    const description = document.getElementById("sellerProductDesc").value.trim();
    const category = document.getElementById("sellerProductCategory").value;
    const price = Number(document.getElementById("sellerProductPrice").value);
    const old_price = Number(document.getElementById("sellerProductOldPrice").value) || 0;
    const stock = Number(document.getElementById("sellerProductStock").value) || 0;
    const image = document.getElementById("sellerProductImage").value.trim();

    if (!name || price <= 0) {
        showToast("Product name and a valid selling price are required.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/seller/products`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, description, category, price, old_price, stock, image })
        });
        const data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Failed to list product.");
            return;
        }

        showToast("✅ Product listed successfully in the marketplace!");
        // Clear form fields
        ["sellerProductName", "sellerProductDesc", "sellerProductPrice", "sellerProductOldPrice", "sellerProductStock", "sellerProductImage"].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = "";
        });
        loadSellerProducts();
        loadProducts(); // refresh main catalog
    } catch (err) {
        showToast("Server error while listing product.");
    }
}

async function sellerDeleteProduct(id) {
    if (!confirm("Remove this product from your listings?")) return;
    try {
        const response = await fetch(`${API_BASE}/products/${id}`, { method: "DELETE" });
        if (response.ok) {
            showToast("Product removed from your listings.");
            loadSellerProducts();
            loadProducts();
        } else {
            const data = await response.json();
            showToast(data.message || "Failed to remove product.");
        }
    } catch (err) {
        showToast("Error removing product.");
    }
}

/* =========================
   UTILITIES
========================= */

function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove("show");
}

function showToast(message) {
    const toast = document.getElementById("toast");
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add("show");
    setTimeout(() => {
        toast.classList.remove("show");
    }, 2800);
}

function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatINR(number) {
    return Number(number).toLocaleString("en-IN", {
        maximumFractionDigits: 2,
        minimumFractionDigits: 0
    });
}

// Start application
checkSession();

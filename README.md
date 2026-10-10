# KalaivaniMart — Full-Stack Java E-Commerce Application

**KalaivaniMart** is a production-ready Indian e-commerce platform built with Java 17, Apache Tomcat 9 (embedded), Servlets/JSP, JDBC, and MySQL (with H2 fallback).

---

## Architecture

| Layer | Technology |
|---|---|
| Backend | Java 17, Servlets, JDBC |
| Server | Embedded Apache Tomcat 9.0.86 |
| Database | MySQL 8+ (cloud/local) / H2 (local fallback) |
| Security | BCrypt password hashing, session-based auth, RBAC |
| Frontend | Vanilla HTML5 / CSS3 / JavaScript (SPA) |
| Build | Apache Maven 3.9.6 |
| Deploy | Docker / Render (render.yaml) |

---

## Features Implemented

### Buyer / Customer
- Registration (buyer/seller roles) with BCrypt password hashing
- Login / Logout with HTTP sessions, session persistence (/api/me)
- Profile view and update (name, phone, address, city, state, pincode)
- Product listing with images (15 seeded Indian products)
- Product search (name + description) and category filter
- Product detail modal with reviews and ratings
- Shopping cart (add, update quantity, remove items) with stock validation
- Checkout with full delivery address and COD order placement
- Transactional order creation (stock decrement + cart clear + order insert)
- Order history with item breakdown and status tracking
- Wishlist (add/remove, toggle heart button)
- Product reviews (rating 1-5, comment, display per product)

### Seller
- Seller account registration and dedicated dashboard
- Add new product / delete own products (ownership enforced)
- RBAC: AuthFilter enforces seller-only access to /api/seller/*

### Admin
- Admin login via dedicated portal
- Dashboard stats: customers, products, orders, revenue
- Product catalog management (add/delete)
- Order management (view all, update status)
- User list (all registered accounts, role display)
- RBAC: AuthFilter blocks non-admins from /api/admin/*

### Infrastructure
- Health endpoint (/health, /api/health) with real DB connectivity check
- H2 embedded fallback when MySQL is offline (file-persisted, MySQL-mode)
- Environment variable configuration: DB_URL, DB_USERNAME, DB_PASSWORD, DB_TYPE
- Docker containerization with Dockerfile
- Render cloud deployment via render.yaml
- Static files served correctly by embedded Tomcat DefaultServlet

---

## Demo Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@kalaivanimart.com | Admin@123 |
| Customer | customer@kalaivanimart.com | Customer@123 |
| Seller | seller@kalaivanimart.com | Seller@123 |

---

## Local Development

### Prerequisites
- JDK 17 (C:\Users\sridh\tools\jdk-17.0.12+7 or any JDK 17 on PATH)
- Maven 3.9.6 (C:\Users\sridh\tools\apache-maven-3.9.6 or any Maven 3.x on PATH)
- MySQL 8 (optional - app falls back to embedded H2)

### Build
```powershell
.\build.ps1
# Or manually:
mvn clean package
```

### Run
```powershell
.\run.ps1
# Or manually:
mvn exec:exec
```

Application URL: http://localhost:8080/kalaivanimart/
Health check: http://localhost:8080/kalaivanimart/health

### Environment Variables
| Variable | Purpose | Default |
|---|---|---|
| DB_URL | JDBC URL for MySQL | jdbc:mysql://localhost:3306/kalaivanimart?... |
| DB_USERNAME | MySQL username | root |
| DB_PASSWORD | MySQL password | (empty) |
| DB_TYPE | Database type | mysql |
| PORT | HTTP port | 8080 |

---

## API Reference

### Public Endpoints
- GET /health - Health + DB status
- GET /api/products - All products (?category=X&search=Y)
- GET /api/products/{id} - Single product detail
- GET /api/reviews?productId={id} - Product reviews
- POST /api/register - Register buyer/seller
- POST /api/login - Buyer/seller login
- POST /api/admin/login - Admin login
- POST /api/logout - Logout
- GET /api/me - Session status

### Buyer (authenticated)
- GET/PUT /api/profile - View/update profile
- GET/POST/PUT/DELETE /api/cart, /api/cart/{id} - Cart management
- GET/POST /api/orders - Order history / place order
- GET/POST/DELETE /api/wishlist, /api/wishlist/{id} - Wishlist
- POST /api/reviews - Submit review

### Seller (role=seller or admin)
- GET /api/seller/products - My listed products
- POST /api/seller/products - Add new product

### Admin (role=admin)
- GET /api/admin/stats - Dashboard stats
- GET/POST /api/admin/products - Catalog management
- PUT/DELETE /api/admin/products/{id} - Edit/delete product
- GET /api/admin/orders - All orders
- PUT /api/admin/orders/{id} - Update order status
- GET /api/admin/users - All registered users

---

## Docker

```bash
docker build -t kalaivanimart .
docker run -p 8080:8080 ^
  -e DB_URL="jdbc:mysql://host:3306/kalaivanimart?..." ^
  -e DB_USERNAME="user" ^
  -e DB_PASSWORD="pass" ^
  kalaivanimart
```

---

## Render Deployment Steps
1. Push your repository to GitHub.
2. In the Render Dashboard, create a New Web Service connected to your repository.
3. Select Docker environment (or let Render detect render.yaml).
4. Set the Health Check path to /kalaivanimart/health.
5. Under Environment Variables, supply your cloud MySQL parameters (DB_URL, DB_USERNAME, DB_PASSWORD, DB_TYPE=mysql).
6. Click Deploy. Render will build the container, provision the service, and verify the health check.

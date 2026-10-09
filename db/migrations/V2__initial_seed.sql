-- ==========================================================
-- MohanMart — Migration V2: Idempotent Initial Seed Data
-- Compatible with PostgreSQL 14+ and H2 2.2.x (ANSI SQL)
-- Note: Admin password in production is overridden by DatabaseSeeder
-- using ADMIN_INITIAL_PASSWORD / ADMIN_PASSWORD (or locked if unset).
-- ==========================================================

INSERT INTO users (name, email, password_hash, role)
SELECT 'Admin Mohan', 'admin@mohanmart.com', '$2a$12$qKzk6H7WGdb2WVpX3bKQmOG9DhO7DnpWKM/Aapv1MW1ylFiwrQkre', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@mohanmart.com');

INSERT INTO users (name, email, password_hash, role)
SELECT 'Mohan Electronics', 'seller1@mohanmart.com', '$2a$12$M6a7NKDtDAoxaoohwBHYOuQTz0NU7PTZYrMPx17Zg2iUVXhM6b0SW', 'SELLER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'seller1@mohanmart.com');

INSERT INTO users (name, email, password_hash, role)
SELECT 'BookWorld Store', 'seller2@mohanmart.com', '$2a$12$M6a7NKDtDAoxaoohwBHYOuQTz0NU7PTZYrMPx17Zg2iUVXhM6b0SW', 'SELLER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'seller2@mohanmart.com');

INSERT INTO users (name, email, password_hash, role)
SELECT 'Alice Buyer', 'buyer1@mohanmart.com', '$2a$12$2PLRursg3L62LU/8ALbIauXz7bAZM6jlXfqiXq33VO1C1ttFavB6u', 'BUYER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'buyer1@mohanmart.com');

INSERT INTO users (name, email, password_hash, role)
SELECT 'Bob Buyer', 'buyer2@mohanmart.com', '$2a$12$2PLRursg3L62LU/8ALbIauXz7bAZM6jlXfqiXq33VO1C1ttFavB6u', 'BUYER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'buyer2@mohanmart.com');

-- Electronics
INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller1@mohanmart.com'),
       'Mechanical Keyboard',
       'Compact TKL mechanical keyboard with Cherry MX Brown switches and per-key RGB backlighting.',
       89.99, 25, 'Electronics',
       'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Mechanical Keyboard');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller1@mohanmart.com'),
       'Wireless Noise-Cancelling Headphones',
       'Over-ear Bluetooth headphones. 40 mm drivers, 30 h battery, foldable design.',
       149.50, 15, 'Electronics',
       'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Wireless Noise-Cancelling Headphones');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller1@mohanmart.com'),
       'USB-C Hub (7-in-1)',
       '4K HDMI, 3x USB-A 3.0, SD card reader, 100 W PD charging.',
       45.00, 50, 'Electronics',
       'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'USB-C Hub (7-in-1)');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller1@mohanmart.com'),
       'Portable Bluetooth Speaker',
       'IPX7 waterproof, 360 degree surround sound, 20 h playtime.',
       59.99, 30, 'Electronics',
       'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Portable Bluetooth Speaker');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller1@mohanmart.com'),
       'Laptop Stand (Aluminium)',
       'Adjustable ergonomic stand compatible with 10-17 inch laptops.',
       34.99, 60, 'Electronics',
       'https://images.unsplash.com/photo-1593642634491-a5fa8eb5b1ac?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Laptop Stand (Aluminium)');

-- Books
INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller2@mohanmart.com'),
       'Clean Code',
       'By Robert C. Martin - A handbook of agile software craftsmanship.',
       39.95, 50, 'Books',
       'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Clean Code');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller2@mohanmart.com'),
       'Effective Java (3rd Edition)',
       'By Joshua Bloch - The definitive guide to best-practice Java programming.',
       44.99, 35, 'Books',
       'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Effective Java (3rd Edition)');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller2@mohanmart.com'),
       'Designing Data-Intensive Applications',
       'By Martin Kleppmann - Deep dive into distributed systems and databases.',
       49.99, 20, 'Books',
       'https://images.unsplash.com/photo-1510172951991-856a654063f9?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Designing Data-Intensive Applications');

-- Home & Kitchen
INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller2@mohanmart.com'),
       'Ceramic Coffee Mug (400 ml)',
       'Handcrafted matte-finish ceramic mug. Dishwasher safe.',
       18.00, 80, 'Home',
       'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Ceramic Coffee Mug (400 ml)');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller2@mohanmart.com'),
       'Bamboo Desk Organiser',
       'Eco-friendly 5-compartment desk organiser made from sustainable bamboo.',
       27.50, 45, 'Home',
       'https://images.unsplash.com/photo-1593642634315-48f5414c3ad9?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Bamboo Desk Organiser');

-- Clothing
INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email = 'seller1@mohanmart.com'),
       'Cotton Graphic Tee - Developer Edition',
       '100% combed cotton. Available in S / M / L / XL. Pre-shrunk.',
       22.00, 100, 'Clothing',
       'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=500'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Cotton Graphic Tee - Developer Edition');

-- Reviews (idempotent by (user_id, product_id))
INSERT INTO reviews (product_id, user_id, rating, comment)
SELECT (SELECT id FROM products WHERE name = 'Mechanical Keyboard'),
       (SELECT id FROM users WHERE email = 'buyer1@mohanmart.com'),
       5,
       'Excellent keyboard! The typing feel is superb and build quality is top-notch.'
WHERE NOT EXISTS (
    SELECT 1 FROM reviews
    WHERE product_id = (SELECT id FROM products WHERE name = 'Mechanical Keyboard')
      AND user_id = (SELECT id FROM users WHERE email = 'buyer1@mohanmart.com')
);

INSERT INTO reviews (product_id, user_id, rating, comment)
SELECT (SELECT id FROM products WHERE name = 'Clean Code'),
       (SELECT id FROM users WHERE email = 'buyer1@mohanmart.com'),
       5,
       'Best programming book I have ever read. A must-have for every developer.'
WHERE NOT EXISTS (
    SELECT 1 FROM reviews
    WHERE product_id = (SELECT id FROM products WHERE name = 'Clean Code')
      AND user_id = (SELECT id FROM users WHERE email = 'buyer1@mohanmart.com')
);

INSERT INTO reviews (product_id, user_id, rating, comment)
SELECT (SELECT id FROM products WHERE name = 'Wireless Noise-Cancelling Headphones'),
       (SELECT id FROM users WHERE email = 'buyer2@mohanmart.com'),
       4,
       'Great sound quality and very comfortable. Battery life is impressive.'
WHERE NOT EXISTS (
    SELECT 1 FROM reviews
    WHERE product_id = (SELECT id FROM products WHERE name = 'Wireless Noise-Cancelling Headphones')
      AND user_id = (SELECT id FROM users WHERE email = 'buyer2@mohanmart.com')
);

INSERT INTO reviews (product_id, user_id, rating, comment)
SELECT (SELECT id FROM products WHERE name = 'Effective Java (3rd Edition)'),
       (SELECT id FROM users WHERE email = 'buyer2@mohanmart.com'),
       5,
       'Bloch explains Java best-practices with clear real-world examples.'
WHERE NOT EXISTS (
    SELECT 1 FROM reviews
    WHERE product_id = (SELECT id FROM products WHERE name = 'Effective Java (3rd Edition)')
      AND user_id = (SELECT id FROM users WHERE email = 'buyer2@mohanmart.com')
);

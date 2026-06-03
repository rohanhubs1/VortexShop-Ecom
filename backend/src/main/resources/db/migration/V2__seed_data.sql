-- V2: Seed Realistic E-Commerce Data
-- Pre-generated BCrypt hash for 'password123' is '$2a$10$/JSIiU8ncMeejIwwDtW75u7aidiUjfndOlqpP0/Uca43ybeQhFhxy'

-- Insert Roles
INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');

-- Insert Admin User (id=1)
INSERT INTO users (email, password, first_name, last_name, phone, active) 
VALUES ('admin@ecom.com', '$2a$10$/JSIiU8ncMeejIwwDtW75u7aidiUjfndOlqpP0/Uca43ybeQhFhxy', 'System', 'Administrator', '+15550100', TRUE);

-- Map Admin User to ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);

-- Insert Customer User (id=2)
INSERT INTO users (email, password, first_name, last_name, phone, active) 
VALUES ('customer@ecom.com', '$2a$10$/JSIiU8ncMeejIwwDtW75u7aidiUjfndOlqpP0/Uca43ybeQhFhxy', 'Alex', 'Developer', '+15550200', TRUE);

-- Map Customer User to ROLE_CUSTOMER
INSERT INTO user_roles (user_id, role_id) VALUES (2, 1);

-- Categories: Electronics (1), Fashion (2), Books (3), Home & Kitchen (4), Gaming (5), Sports (6)
INSERT INTO categories (name, slug, description) VALUES 
('Electronics', 'electronics', 'Smartphones, laptops, cameras, and audio devices from top brands.'),
('Fashion', 'fashion', 'Trendy clothing, shoes, and accessories for men and women.'),
('Books', 'books', 'Bestsellers, fiction, non-fiction, and educational material.'),
('Home & Kitchen', 'home-kitchen', 'Furniture, decor, appliances, and cookware to elevate your living space.'),
('Gaming', 'gaming', 'Consoles, PC gaming hardware, accessories, and popular games.'),
('Sports', 'sports', 'Activewear, equipment, and accessories for outdoor and indoor sports.');

-- 1. Electronics (Category ID: 1)
INSERT INTO products (category_id, name, slug, description, price, stock_quantity, image_url, average_rating) VALUES
(1, 'Sony Alpha a7 III Mirrorless Camera', 'sony-alpha-a7-iii', 'Full-frame mirrorless camera with 24.2MP BSI image sensor and high-speed continuous shooting.', 1998.00, 15, 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800', 4.8),
(1, 'Apple MacBook Pro 16" M3 Max', 'apple-macbook-pro-16-m3-max', 'The most powerful MacBook Pro ever. Features the M3 Max chip, 36GB Unified Memory, and 1TB SSD.', 3499.00, 20, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', 4.9),
(1, 'Bose QuietComfort Ultra Headphones', 'bose-qc-ultra-headphones', 'Premium wireless noise-cancelling headphones with spatial audio and world-class ANC.', 429.00, 45, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800', 4.7),
(1, 'Samsung Galaxy S24 Ultra', 'samsung-galaxy-s24-ultra', 'Titanium design, 200MP camera, and built-in AI features. 512GB storage.', 1299.99, 30, 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800', 4.6),
(1, 'Logitech MX Master 3S', 'logitech-mx-master-3s', 'Advanced wireless mouse with 8K DPI any-surface tracking and quiet clicks.', 99.99, 100, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800', 4.8),
(1, 'Keychron Q1 Pro Mechanical Keyboard', 'keychron-q1-pro', 'QMK/VIA wireless custom mechanical keyboard with a 75% layout and CNC aluminum body.', 199.00, 50, 'https://images.unsplash.com/photo-1595225476474-87563907a212?w=800', 4.7),
(1, 'LG C3 65-Inch OLED TV', 'lg-c3-65-oled-tv', 'Stunning 4K OLED evo Smart TV with 120Hz refresh rate and Dolby Vision.', 1599.99, 10, 'https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=800', 4.9),
(1, 'Anker 737 Power Bank', 'anker-737-power-bank', '24,000mAh 3-port portable charger with 140W output and smart digital display.', 149.99, 60, 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=800', 4.5);

-- 2. Fashion (Category ID: 2)
INSERT INTO products (category_id, name, slug, description, price, stock_quantity, image_url, average_rating) VALUES
(2, 'Classic White Leather Sneakers', 'classic-white-leather-sneakers', 'Minimalist white sneakers made from premium Italian leather. Perfect for everyday wear.', 120.00, 80, 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=800', 4.6),
(2, 'Men''s Slim Fit Denim Jacket', 'mens-slim-fit-denim-jacket', 'Vintage wash denim jacket with a tailored fit and silver-tone hardware.', 85.00, 60, 'https://images.unsplash.com/photo-1495105787522-5334e3ffa0ef?w=800', 4.4),
(2, 'Women''s Cashmere Turtleneck Sweater', 'womens-cashmere-turtleneck', 'Luxuriously soft 100% Mongolian cashmere sweater in a relaxed fit.', 150.00, 40, 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800', 4.8),
(2, 'Oversized Vintage Graphic Tee', 'oversized-vintage-graphic-tee', 'Heavyweight cotton t-shirt with a faded vintage band graphic.', 35.00, 150, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800', 4.3),
(2, 'Unisex Aviator Sunglasses', 'unisex-aviator-sunglasses', 'Gold-tone metal frame aviators with polarized dark green lenses.', 145.00, 50, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=800', 4.7),
(2, 'Athletic Performance Joggers', 'athletic-performance-joggers', 'Moisture-wicking, four-way stretch fabric with zip pockets for active wear.', 65.00, 90, 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800', 4.5),
(2, 'Minimalist Leather Tote Bag', 'minimalist-leather-tote-bag', 'Spacious everyday tote crafted from vegetable-tanned leather.', 195.00, 25, 'https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=800', 4.8);

-- 3. Books (Category ID: 3)
INSERT INTO products (category_id, name, slug, description, price, stock_quantity, image_url, average_rating) VALUES
(3, 'Designing Data-Intensive Applications', 'designing-data-intensive-apps', 'The definitive guide to distributed systems, database scaling, and reliability.', 45.99, 120, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800', 4.9),
(3, 'Atomic Habits', 'atomic-habits', 'An easy and proven way to build good habits and break bad ones.', 16.99, 200, 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=800', 4.8),
(3, 'Clean Architecture', 'clean-architecture', 'A Craftsman''s Guide to Software Structure and Design by Robert C. Martin.', 39.99, 85, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800', 4.7),
(3, 'The Psychology of Money', 'the-psychology-of-money', 'Timeless lessons on wealth, greed, and happiness.', 18.50, 150, 'https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=800', 4.6),
(3, 'Dune: Deluxe Edition', 'dune-deluxe-edition', 'The science fiction masterpiece, featuring stunning cover art and sprayed edges.', 29.99, 70, 'https://images.unsplash.com/photo-1618666012174-83b441c0bc76?w=800', 4.9),
(3, 'Sapiens: A Brief History of Humankind', 'sapiens-history-humankind', 'A narrative of humanity''s creation and evolution by Yuval Noah Harari.', 22.00, 110, 'https://images.unsplash.com/photo-1588666309990-d68f08e3d4a6?w=800', 4.5),
(3, 'Meditations by Marcus Aurelius', 'meditations-marcus-aurelius', 'The timeless philosophical diary of the Roman Emperor.', 12.99, 300, 'https://images.unsplash.com/photo-1522045610051-50e58d4a9937?w=800', 4.7);

-- 4. Home & Kitchen (Category ID: 4)
INSERT INTO products (category_id, name, slug, description, price, stock_quantity, image_url, average_rating) VALUES
(4, 'Chemex Pour-Over Glass Coffeemaker', 'chemex-pour-over', 'Classic 8-cup glass coffee maker with wood collar and tie.', 49.50, 60, 'https://images.unsplash.com/photo-1517701550927-30cfcb64db18?w=800', 4.8),
(4, 'Le Creuset Enameled Cast Iron Dutch Oven', 'le-creuset-dutch-oven', '5.5-quart round dutch oven in Cerise red. Exceptional heat retention.', 419.95, 15, 'https://images.unsplash.com/photo-1584286595398-a59f21d313f5?w=800', 4.9),
(4, 'Ninja Air Fryer Max XL', 'ninja-air-fryer-max-xl', '5.5-quart air fryer that cooks, crisps, roasts, bakes, and reheats.', 129.99, 85, 'https://images.unsplash.com/photo-1626201319409-f81d115e5b31?w=800', 4.7),
(4, 'Vitamix 5200 Professional Blender', 'vitamix-5200-blender', 'High-performance blender with 64 oz container, perfect for smoothies and soups.', 479.95, 25, 'https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=800', 4.8),
(4, 'SMEG Retro Electric Kettle', 'smeg-retro-kettle', '50s retro style electric kettle in pastel green. 1.7L capacity.', 189.95, 30, 'https://images.unsplash.com/photo-1588147775988-cb58b76dfb37?w=800', 4.6),
(4, 'Modern Ceramic Vase Trio', 'modern-ceramic-vase-trio', 'Set of 3 matte white ceramic vases for minimalist floral arrangements.', 45.00, 100, 'https://images.unsplash.com/photo-1612224322409-a178bc9d21ce?w=800', 4.5),
(4, 'Wusthof Classic 8-Inch Chef''s Knife', 'wusthof-classic-chef-knife', 'Forged high-carbon stainless steel kitchen knife. Precision edge technology.', 170.00, 45, 'https://images.unsplash.com/photo-1593618998160-e34014e67546?w=800', 4.9),
(4, 'Aesthetic Bamboo Bath Tray', 'bamboo-bath-tray', 'Expandable bathtub caddy tray with reading rack, tablet holder, and wine glass slot.', 39.99, 150, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800', 4.4);

-- 5. Gaming (Category ID: 5)
INSERT INTO products (category_id, name, slug, description, price, stock_quantity, image_url, average_rating) VALUES
(5, 'PlayStation 5 Console', 'playstation-5-console', 'Next-gen gaming with ultra-high speed SSD, ray tracing, and haptic feedback.', 499.99, 50, 'https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=800', 4.9),
(5, 'Xbox Series X', 'xbox-series-x', 'The fastest, most powerful Xbox ever. True 4K gaming, up to 120 FPS.', 499.99, 45, 'https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=800', 4.8),
(5, 'Nintendo Switch OLED Model', 'nintendo-switch-oled', 'Play at home or on the go with a vibrant 7-inch OLED screen.', 349.99, 75, 'https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?w=800', 4.8),
(5, 'Razer DeathAdder V3 Pro', 'razer-deathadder-v3-pro', 'Ultra-lightweight wireless esports gaming mouse with 30K optical sensor.', 149.99, 90, 'https://images.unsplash.com/photo-1615663245857-ac1e653815f7?w=800', 4.7),
(5, 'SteelSeries Arctis Nova Pro Wireless', 'steelseries-arctis-nova-pro', 'Multi-system wireless gaming headset with active noise cancellation.', 349.99, 35, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800', 4.6),
(5, 'ASUS ROG Swift 27" 1440p 240Hz Monitor', 'asus-rog-swift-27-monitor', 'Esports gaming monitor with OLED panel, 0.03ms response time.', 899.00, 20, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', 4.8),
(5, 'Secretlab TITAN Evo Gaming Chair', 'secretlab-titan-evo', 'Premium ergonomic gaming chair with magnetic memory foam head pillow.', 549.00, 15, 'https://images.unsplash.com/photo-1505843490538-5133c6c7d0e1?w=800', 4.7);

-- 6. Sports (Category ID: 6)
INSERT INTO products (category_id, name, slug, description, price, stock_quantity, image_url, average_rating) VALUES
(6, 'Manduka PRO Yoga Mat', 'manduka-pro-yoga-mat', 'Ultra-dense cushioning, non-slip performance, and lifetime guarantee.', 129.00, 60, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800', 4.8),
(6, 'Bowflex SelectTech 552 Adjustable Dumbbells', 'bowflex-552-dumbbells', 'Adjusts from 5 to 52.5 lbs. Replaces 15 sets of weights.', 429.00, 25, 'https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800', 4.9),
(6, 'Hydro Flask 32 oz Wide Mouth', 'hydro-flask-32oz', 'Double wall vacuum insulated stainless steel water bottle. Keeps liquids cold for 24 hours.', 44.95, 200, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800', 4.7),
(6, 'Theragun Pro Percussive Therapy Device', 'theragun-pro', 'Deep muscle treatment for enhanced recovery, performance, and sleep.', 599.00, 30, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800', 4.8),
(6, 'Spalding Official NBA Game Basketball', 'spalding-nba-basketball', 'Full grain leather cover, official size and weight. Indoor use only.', 169.99, 45, 'https://images.unsplash.com/photo-1519861531473-9200262188bf?w=800', 4.9),
(6, 'Garmin Forerunner 965', 'garmin-forerunner-965', 'Premium GPS running and triathlon smartwatch with AMOLED display.', 599.99, 40, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800', 4.7);

-- Insert Customer Billing & Shipping Address
INSERT INTO addresses (user_id, title, street_address, city, state, zip_code, country, is_default) VALUES
(2, 'Home', '123 Tech Lane', 'San Francisco', 'CA', '94105', 'United States', TRUE);

-- Insert Sample Coupons
INSERT INTO coupons (code, discount_percentage, max_discount, minimum_order_amount, expiry_date, active) VALUES
('WELCOME10', 10, 50.00, 30.00, '2029-12-31 23:59:59', TRUE),
('MEGA20', 20, 100.00, 100.00, '2029-12-31 23:59:59', TRUE);

-- Insert Sample Reviews for Products
INSERT INTO reviews (user_id, product_id, rating, comment) VALUES
(2, 2, 5, 'The M3 Max is an absolute beast for software development and video editing. Worth every penny.'),
(2, 5, 4, 'Great mouse, very ergonomic. The scroll wheel is fantastic.'),
(2, 9, 5, 'Super comfortable and stylish. Gets dirty easily though.');

-- Seed mock sales counts to populate the "Most Ordered" feature
UPDATE products SET sales_count = 145 WHERE id = 1;
UPDATE products SET sales_count = 89 WHERE id = 2;
UPDATE products SET sales_count = 312 WHERE id = 3;
UPDATE products SET sales_count = 56 WHERE id = 4;
UPDATE products SET sales_count = 278 WHERE id = 5;
UPDATE products SET sales_count = 190 WHERE id = 6;
UPDATE products SET sales_count = 24 WHERE id = 7;
UPDATE products SET sales_count = 412 WHERE id = 8;
UPDATE products SET sales_count = 67 WHERE id = 9;
UPDATE products SET sales_count = 123 WHERE id = 10;
UPDATE products SET sales_count = 45 WHERE id = 11;
UPDATE products SET sales_count = 88 WHERE id = 12;
UPDATE products SET sales_count = 14 WHERE id = 13;
UPDATE products SET sales_count = 305 WHERE id = 14;
UPDATE products SET sales_count = 92 WHERE id = 15;
UPDATE products SET sales_count = 201 WHERE id = 16;
UPDATE products SET sales_count = 34 WHERE id = 17;
UPDATE products SET sales_count = 76 WHERE id = 18;
UPDATE products SET sales_count = 150 WHERE id = 19;
UPDATE products SET sales_count = 230 WHERE id = 20;

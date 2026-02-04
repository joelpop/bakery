-- Demo seed data for Café Sunshine Bakery
-- This file is automatically loaded by Spring Boot in development mode

-- Users (passwords are BCrypt hashed)
-- admin123, baker123, barista123
INSERT INTO app_user (id, version, email, first_name, last_name, password_hash, role, created_at) VALUES
(1, 0, 'admin@cafe-sunshine.com', 'Admin', 'User', '$2b$10$uIr3nuDde8Cf9QE8QI4nHuznvLV/70tbUKspP2J0tZg4kNhcKuiAa', 'ADMIN', CURRENT_TIMESTAMP),
(2, 0, 'baker@cafe-sunshine.com', 'Baker', 'Smith', '$2b$10$cXHTiUndgoMk1l0kXdzwg.Q3M4gC1F.uw5RvmJS8NlXf/rLOPeyWO', 'BAKER', CURRENT_TIMESTAMP),
(3, 0, 'barista@cafe-sunshine.com', 'Barista', 'Jones', '$2b$10$AMuiGV50FqtUCGF/FQu5IeALsi/qhrNCXynRxmA/SZKtMK9CkiZSG', 'BARISTA', CURRENT_TIMESTAMP);

-- Locations (with timezone for display conversion)
INSERT INTO location (id, version, name, address, timezone, active, sort_order, created_at) VALUES
(1, 0, 'Downtown Store', '123 Main Street, Downtown', 'America/New_York', true, 1, CURRENT_TIMESTAMP),
(2, 0, 'Central Bakery', '456 Baker Lane', 'America/New_York', true, 2, CURRENT_TIMESTAMP);

-- Products (baked goods)
INSERT INTO product (id, version, name, description, size, price, available, created_at) VALUES
-- Pastries
(1, 0, 'Croissant', 'Buttery, flaky French pastry', 'Individual', 3.50, true, CURRENT_TIMESTAMP),
(2, 0, 'Chocolate Croissant', 'Croissant filled with rich chocolate', 'Individual', 4.00, true, CURRENT_TIMESTAMP),
(3, 0, 'Almond Croissant', 'Croissant with almond cream and sliced almonds', 'Individual', 4.50, true, CURRENT_TIMESTAMP),
(4, 0, 'Cinnamon Roll', 'Fresh-baked with cream cheese frosting', 'Individual', 4.50, true, CURRENT_TIMESTAMP),
(5, 0, 'Blueberry Scone', 'Loaded with fresh blueberries', 'Individual', 3.25, true, CURRENT_TIMESTAMP),
(6, 0, 'Chocolate Muffin', 'Rich chocolate chip muffin', 'Individual', 3.00, true, CURRENT_TIMESTAMP),
(7, 0, 'Banana Nut Muffin', 'Moist banana muffin with walnuts', 'Individual', 3.25, true, CURRENT_TIMESTAMP),
-- Breads
(8, 0, 'Sourdough Loaf', 'Artisan sourdough bread', 'Large', 7.00, true, CURRENT_TIMESTAMP),
(9, 0, 'Baguette', 'Classic French bread', 'Regular', 4.00, true, CURRENT_TIMESTAMP),
(10, 0, 'Ciabatta', 'Italian flatbread with olive oil', 'Regular', 4.50, true, CURRENT_TIMESTAMP),
(11, 0, 'Whole Wheat Loaf', 'Hearty whole grain bread', 'Large', 6.00, true, CURRENT_TIMESTAMP),
(12, 0, 'Focaccia', 'Italian herb bread with rosemary', 'Half Sheet', 8.00, true, CURRENT_TIMESTAMP),
-- Cakes and Tarts
(13, 0, 'Birthday Cake', 'Vanilla layer cake with buttercream', '12 people', 45.00, true, CURRENT_TIMESTAMP),
(14, 0, 'Chocolate Cake', 'Rich chocolate layer cake', '12 people', 48.00, true, CURRENT_TIMESTAMP),
(15, 0, 'Fruit Tart', 'Fresh seasonal fruit on pastry cream', '8 people', 28.00, true, CURRENT_TIMESTAMP),
(16, 0, 'Cheesecake', 'New York style cheesecake', '10 people', 35.00, true, CURRENT_TIMESTAMP),
-- Specialty Items
(17, 0, 'Quiche Lorraine', 'Bacon and cheese quiche', '6 people', 22.00, true, CURRENT_TIMESTAMP),
(18, 0, 'Danish Pastry', 'Fruit-filled flaky pastry', 'Individual', 3.75, true, CURRENT_TIMESTAMP);

-- Customers
INSERT INTO customer (id, version, name, phone_number, email, active, created_at) VALUES
(1, 0, 'Alice Johnson', '555-0101', 'alice@example.com', true, CURRENT_TIMESTAMP),
(2, 0, 'Bob Smith', '555-0102', 'bob@example.com', true, CURRENT_TIMESTAMP),
(3, 0, 'Carol White', '555-0103', 'carol@example.com', true, CURRENT_TIMESTAMP),
(4, 0, 'David Brown', '555-0104', 'david@example.com', true, CURRENT_TIMESTAMP),
(5, 0, 'Emma Davis', '555-0105', 'emma@example.com', true, CURRENT_TIMESTAMP),
(6, 0, 'Frank Miller', '555-0106', 'frank@example.com', true, CURRENT_TIMESTAMP),
(7, 0, 'Grace Lee', '555-0107', 'grace@example.com', true, CURRENT_TIMESTAMP),
(8, 0, 'Henry Wilson', '555-0108', 'henry@example.com', true, CURRENT_TIMESTAMP),
(9, 0, 'Iris Martinez', '555-0109', 'iris@example.com', true, CURRENT_TIMESTAMP);

-- Orders with various statuses
-- Downtown Store pickups (location_id = 1)
-- Order 1: NEW - Alice, due today at 14:00
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(1, 0, 'NEW', CURRENT_DATE, '14:00:00', 10.25, false, CURRENT_TIMESTAMP, 1, 1);

-- Order 2: NEW - Bob, due tomorrow at 10:00
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(2, 0, 'NEW', CURRENT_DATE + 1, '10:00:00', 7.00, false, CURRENT_TIMESTAMP, 2, 1);

-- Order 3: VERIFIED - Carol, due tomorrow at 12:00
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(3, 0, 'VERIFIED', CURRENT_DATE + 1, '12:00:00', 13.00, false, CURRENT_TIMESTAMP, 3, 1);

-- Order 4: IN_PROGRESS - David, due today at 16:00 (birthday cake)
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(4, 0, 'IN_PROGRESS', CURRENT_DATE, '16:00:00', 45.00, false, CURRENT_TIMESTAMP, 4, 1);

-- Order 5: READY_FOR_PICK_UP - Emma, due today at 11:00
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(5, 0, 'READY_FOR_PICK_UP', CURRENT_DATE, '11:00:00', 17.00, false, CURRENT_TIMESTAMP, 5, 1);

-- Order 6: PICKED_UP - Alice, yesterday at 15:00
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(6, 0, 'PICKED_UP', CURRENT_DATE - 1, '15:00:00', 28.00, true, CURRENT_TIMESTAMP - INTERVAL '1' DAY, 1, 1);

-- Order 7: PICKED_UP - Bob, two days ago
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(7, 0, 'PICKED_UP', CURRENT_DATE - 2, '09:00:00', 21.00, true, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 2, 1);

-- Order 8: CANCELLED - Carol, due today
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(8, 0, 'CANCELLED', CURRENT_DATE, '13:00:00', 45.00, false, CURRENT_TIMESTAMP, 3, 1);

-- Central Bakery pickups (location_id = 2)
-- Order 9: NEW - Frank, due today at 15:00 (large bread order)
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(9, 0, 'NEW', CURRENT_DATE, '15:00:00', 33.00, false, CURRENT_TIMESTAMP, 6, 2);

-- Order 10: VERIFIED - Grace, due tomorrow at 09:00 (catering pastries)
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(10, 0, 'VERIFIED', CURRENT_DATE + 1, '09:00:00', 45.00, false, CURRENT_TIMESTAMP, 7, 2);

-- Order 11: IN_PROGRESS - Henry, due today at 17:00 (chocolate cake)
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(11, 0, 'IN_PROGRESS', CURRENT_DATE, '17:00:00', 48.00, true, CURRENT_TIMESTAMP, 8, 2);

-- Order 12: BAKED - Iris, due today at 12:00 (quiche for lunch)
INSERT INTO customer_order (id, version, status, due_date, due_time, total, paid, created_at, customer_id, location_id) VALUES
(12, 0, 'BAKED', CURRENT_DATE, '12:00:00', 22.00, false, CURRENT_TIMESTAMP, 9, 2);

-- Order Items (with status matching order status where applicable)
-- Order 1: NEW - 2 Croissants ($7.00), 1 Blueberry Scone ($3.25) = $10.25
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(1, 0, 'NEW', 2, 3.50, 7.00, 1, 1),
(2, 0, 'NEW', 1, 3.25, 3.25, 1, 5);

-- Order 2: NEW - 1 Sourdough Loaf ($7.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(3, 0, 'NEW', 1, 7.00, 7.00, 2, 8);

-- Order 3: VERIFIED - 3 Chocolate Muffins ($9.00), 1 Baguette ($4.00) = $13.00
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(4, 0, 'VERIFIED', 3, 3.00, 9.00, 3, 6),
(5, 0, 'VERIFIED', 1, 4.00, 4.00, 3, 9);

-- Order 4: IN_PROGRESS - 1 Birthday Cake ($45.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(6, 0, 'IN_PROGRESS', 1, 45.00, 45.00, 4, 13);

-- Order 5: READY - 2 Baguettes ($8.00), 2 Cinnamon Rolls ($9.00) = $17.00
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(7, 0, 'BAKED', 2, 4.00, 8.00, 5, 9),
(8, 0, 'BAKED', 2, 4.50, 9.00, 5, 4);

-- Order 6: PICKED_UP - 1 Fruit Tart ($28.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(9, 0, 'BAKED', 1, 28.00, 28.00, 6, 15);

-- Order 7: PICKED_UP - 6 Croissants ($21.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(10, 0, 'BAKED', 6, 3.50, 21.00, 7, 1);

-- Order 8: CANCELLED - 1 Birthday Cake ($45.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(11, 0, 'CANCELLED', 1, 45.00, 45.00, 8, 13);

-- Order 9: NEW - Bread assortment for bakery pickup
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(12, 0, 'NEW', 2, 7.00, 14.00, 9, 8),
(13, 0, 'NEW', 2, 4.00, 8.00, 9, 9),
(14, 0, 'NEW', 1, 4.50, 4.50, 9, 10),
(15, 0, 'NEW', 1, 6.00, 6.00, 9, 11);

-- Order 10: VERIFIED - Catering pastries for bakery pickup
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(16, 0, 'VERIFIED', 6, 3.50, 21.00, 10, 1),
(17, 0, 'VERIFIED', 6, 4.00, 24.00, 10, 2);

-- Order 11: IN_PROGRESS - Henry's Chocolate Cake ($48.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(18, 0, 'IN_PROGRESS', 1, 48.00, 48.00, 11, 14);

-- Order 12: BAKED - Iris's Quiche Lorraine ($22.00)
INSERT INTO order_item (id, version, status, quantity, unit_price, line_total, order_id, product_id) VALUES
(19, 0, 'BAKED', 1, 22.00, 22.00, 12, 17);

-- users 테이블에 초기 사용자 데이터 삽입
INSERT INTO users (username, email) VALUES
                                        ('john_doe', 'john.doe@example.com'),
                                        ('jane_smith', 'jane.smith@example.com'),
                                        ('alice_jones', 'alice.jones@example.com');

-- products 테이블에 초기 상품 데이터 삽입
INSERT INTO product (name, description, price, stock) VALUES
                                                          ('Laptop', 'High performance laptop', 1500.00, 10),
                                                          ('Smartphone', 'Latest model smartphone', 800.00, 20),
                                                          ('Headphones', 'Noise-cancelling headphones', 200.00, 15),
                                                          ('Monitor', '4K Ultra HD monitor', 400.00, 8),
                                                          ('Keyboard', 'Mechanical keyboard', 100.00, 25);

-- orders 테이블에 초기 주문 데이터 삽입
INSERT INTO orders (order_date, user_id, product_id, quantity, status, total_amount) VALUES
                                                                                         ('2024-01-15 10:30:00', 1, 1, 2, 'PENDING', 3000.00),
                                                                                         ('2024-02-20 14:45:00', 2, 3, 1, 'COMPLETED', 200.00),
                                                                                         ('2024-03-05 09:15:00', 1, 2, 3, 'CANCELED', 2400.00),
                                                                                         ('2024-04-10 16:00:00', 3, 4, 1, 'PENDING', 400.00),
                                                                                         ('2024-05-25 11:20:00', 2, 5, 5, 'COMPLETED', 500.00);

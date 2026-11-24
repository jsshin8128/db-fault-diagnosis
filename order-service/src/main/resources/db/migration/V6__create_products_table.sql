CREATE TABLE products (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(50)    NOT NULL,
    category VARCHAR(30)    NOT NULL,
    price    DECIMAL(12, 2) NOT NULL
);

INSERT INTO products (name, category, price)
WITH RECURSIVE digits (n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM digits WHERE n < 9)
SELECT CONCAT('product-', a.n + 10 * b.n + 100 * c.n + 1000 * d.n + 1),
       ELT(FLOOR(RAND() * 8) + 1, 'BOOKS', 'ELECTRONICS', 'FASHION', 'FOOD', 'HOME', 'SPORTS', 'TOYS', 'BEAUTY'),
       ROUND(RAND() * 300000, 2)
FROM digits a, digits b, digits c, digits d;

CREATE INDEX idx_products_category ON products (category);

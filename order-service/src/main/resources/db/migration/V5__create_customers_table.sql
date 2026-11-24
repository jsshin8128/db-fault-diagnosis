CREATE TABLE customers (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    grade      VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL
);

INSERT INTO customers (name, email, grade, created_at)
WITH RECURSIVE digits (n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM digits WHERE n < 9)
SELECT CONCAT('customer-', a.n + 10 * b.n + 100 * c.n + 1000 * d.n + 10000 * e.n + 100000 * f.n + 1),
       CONCAT('customer-', a.n + 10 * b.n + 100 * c.n + 1000 * d.n + 10000 * e.n + 100000 * f.n + 1, '@example.com'),
       ELT(FLOOR(RAND() * 4) + 1, 'BASIC', 'SILVER', 'GOLD', 'VIP'),
       NOW(6) - INTERVAL FLOOR(RAND() * 1000) DAY
FROM digits a, digits b, digits c, digits d, digits e,
     (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) f;

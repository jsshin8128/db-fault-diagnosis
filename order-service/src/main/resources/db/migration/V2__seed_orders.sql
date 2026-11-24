INSERT INTO orders (customer_id, status, total_amount, ordered_at)
WITH RECURSIVE digits (n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM digits WHERE n < 9)
SELECT FLOOR(RAND() * 500000) + 1,
       ELT(FLOOR(RAND() * 5) + 1, 'CREATED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'),
       ROUND(RAND() * 500000, 2),
       NOW(6) - INTERVAL FLOOR(RAND() * 365) DAY
FROM digits a, digits b, digits c, digits d, digits e, digits f,
     (SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) g;

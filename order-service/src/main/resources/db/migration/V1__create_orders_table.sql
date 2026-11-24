CREATE TABLE orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT         NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    ordered_at   DATETIME(6)    NOT NULL
);

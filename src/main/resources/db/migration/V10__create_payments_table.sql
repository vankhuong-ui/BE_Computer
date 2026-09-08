CREATE TABLE payments (
                          id BIGINT IDENTITY(1,1) PRIMARY KEY,

                          order_id BIGINT NOT NULL,

                          transaction_id VARCHAR(255),

                          payment_gateway VARCHAR(50) NOT NULL,

                          amount DECIMAL(18,2) NOT NULL,

                          status VARCHAR(20) NOT NULL,

                          response_code VARCHAR(20),

                          created_at DATETIME2,

                          CONSTRAINT FK_payments_orders
                              FOREIGN KEY (order_id)
                                  REFERENCES orders(id),

                          CONSTRAINT CK_payments_status
                              CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

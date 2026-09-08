CREATE TABLE orders (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,

                        useraddresses_id BIGINT NULL,

                        total_amount DECIMAL(19,2) NULL,

                        status VARCHAR(255) NULL,

                        created_at DATETIME2 NULL,

                        CONSTRAINT FK_orders_user_addresses
                            FOREIGN KEY (useraddresses_id)
                                REFERENCES user_addresses(id)
);
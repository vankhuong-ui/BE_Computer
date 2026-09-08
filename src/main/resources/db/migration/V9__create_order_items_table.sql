CREATE TABLE order_items (
                             id BIGINT IDENTITY(1,1) PRIMARY KEY,

                             order_id BIGINT NOT NULL,

                             product_id BIGINT NOT NULL,

                             quantity INT NOT NULL,

                             price DECIMAL(19,2) NULL,

                             CONSTRAINT FK_order_items_orders
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(id),

                             CONSTRAINT FK_order_items_products
                                 FOREIGN KEY (product_id)
                                     REFERENCES products(id)
);
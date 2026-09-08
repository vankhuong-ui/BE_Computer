CREATE TABLE reviews (
                         id BIGINT IDENTITY(1,1) NOT NULL,
                         order_id BIGINT NOT NULL,
                         product_id BIGINT NOT NULL,
                         rating INT NOT NULL,
                         comment NVARCHAR(MAX) NULL,
                         created_at DATETIME2 NULL,

                         CONSTRAINT PK_reviews PRIMARY KEY (id),

                         CONSTRAINT FK_reviews_orders
                             FOREIGN KEY (order_id)
                                 REFERENCES orders(id),

                         CONSTRAINT FK_reviews_products
                             FOREIGN KEY (product_id)
                                 REFERENCES products(id),

                         CONSTRAINT CK_reviews_rating
                             CHECK (rating >= 1 AND rating <= 5)
);

CREATE TABLE cart (
                      id BIGINT IDENTITY(1,1) PRIMARY KEY,

                      product_id BIGINT,
                      account_id BIGINT,
                      quality INT,

                      CONSTRAINT FK_cart_products
                          FOREIGN KEY (product_id)
                              REFERENCES products(id),

                      CONSTRAINT FK_cart_account
                          FOREIGN KEY (account_id)
                              REFERENCES account(id)
);
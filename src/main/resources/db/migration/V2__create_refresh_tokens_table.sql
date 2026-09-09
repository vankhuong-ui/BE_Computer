CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token VARCHAR(500) NOT NULL UNIQUE,
                                account_id BIGINT NOT NULL,
                                expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,

                                CONSTRAINT fk_refresh_token_account
                                    FOREIGN KEY (account_id)
                                        REFERENCES account(id)
);

CREATE INDEX idx_refresh_tokens_account_id
    ON refresh_tokens(account_id);

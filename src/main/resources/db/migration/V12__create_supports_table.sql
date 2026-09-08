CREATE TABLE supports (
                          id BIGINT IDENTITY(1,1) PRIMARY KEY,
                          account_id BIGINT NOT NULL,
                          subject NVARCHAR(255) NOT NULL,
                          message NVARCHAR(MAX) NOT NULL,
                          status VARCHAR(20) NOT NULL CONSTRAINT DF_supports_status DEFAULT 'OPEN',
                          created_at DATETIME2 NOT NULL CONSTRAINT DF_supports_created_at DEFAULT GETDATE(),

    -- Khóa ngoại liên kết tới bảng account
                          CONSTRAINT FK_supports_account FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE
);
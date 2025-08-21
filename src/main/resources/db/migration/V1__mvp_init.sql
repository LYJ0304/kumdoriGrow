-- USERS
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- RECEIPTS
CREATE TABLE IF NOT EXISTS receipts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_name VARCHAR(255) NULL,
    total_amount BIGINT NULL,
    category ENUM('FRANCHISE','LOCAL','MARKET') NULL,
    exp_awarded INT NULL,
    ocr_raw JSON NULL,
    image_path VARCHAR(512) NULL,
    recognized_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipts_user FOREIGN KEY (user_id) REFERENCES users(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_receipts_user_time ON receipts(user_id, recognized_at DESC);
CREATE INDEX idx_receipts_category ON receipts(category);

-- USER_EXPERIENCE
CREATE TABLE IF NOT EXISTS user_experience (
    user_id BIGINT PRIMARY KEY,
    total_exp BIGINT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_exp_user FOREIGN KEY (user_id) REFERENCES users(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- XP_LEDGER
CREATE TABLE IF NOT EXISTS xp_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receipt_id BIGINT NULL,
    delta_exp INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_xp_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_xp_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_xp_ledger_user_time ON xp_ledger(user_id, created_at DESC);
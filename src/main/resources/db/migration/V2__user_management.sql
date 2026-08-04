-- Migration script for User Management, Audit Log, and OTP

-- Note: In H2 in-memory, Hibernate ddl-auto will generate these, 
-- but this is provided for MySQL production deployment.

-- Update users table with new columns if they exist, or create table
-- Since the table already exists, we will alter it.
-- Depending on SQL dialect, this is standard SQL.

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(255) NOT NULL DEFAULT 'Admin',
ADD COLUMN IF NOT EXISTS last_name VARCHAR(255) NOT NULL DEFAULT 'User',
ADD COLUMN IF NOT EXISTS email VARCHAR(255) UNIQUE,
ADD COLUMN IF NOT EXISTS phone VARCHAR(255),
ADD COLUMN IF NOT EXISTS department VARCHAR(255),
ADD COLUMN IF NOT EXISTS job_title VARCHAR(255),
ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS avatar VARCHAR(255),
ADD COLUMN IF NOT EXISTS created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_login TIMESTAMP,
ADD COLUMN IF NOT EXISTS require_password_change BOOLEAN NOT NULL DEFAULT FALSE;

-- Map existing roles (QA, VIEWER) to MANAGER, USER if needed.
-- In a real migration you would do:
-- UPDATE users SET role = 'MANAGER' WHERE role = 'QA';
-- UPDATE users SET role = 'USER' WHERE role = 'VIEWER';

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    time TIMESTAMP NOT NULL,
    ip_address VARCHAR(255),
    browser VARCHAR(255),
    details TEXT
);

CREATE TABLE IF NOT EXISTS password_reset_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(255),
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast OTP lookups
CREATE INDEX IF NOT EXISTS idx_otp_email ON password_reset_otp(email);
CREATE INDEX IF NOT EXISTS idx_otp_user_id ON password_reset_otp(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_username ON audit_log(user_name);

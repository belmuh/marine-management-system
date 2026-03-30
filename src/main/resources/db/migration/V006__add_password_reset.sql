-- Add password reset fields to users table
ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(100);
ALTER TABLE users ADD COLUMN password_reset_token_expires_at TIMESTAMP;

-- Index for fast token lookups (partial index - only non-null tokens)
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token) WHERE password_reset_token IS NOT NULL;

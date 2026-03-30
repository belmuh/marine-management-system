-- Add email verification fields to users table
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN verification_token VARCHAR(100);
ALTER TABLE users ADD COLUMN verification_token_expires_at TIMESTAMP;

-- Add index for verification token lookups
CREATE INDEX idx_users_verification_token ON users(verification_token) WHERE verification_token IS NOT NULL;

-- Add onboarding_completed to organizations table
ALTER TABLE organizations ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT false;

-- Mark existing users as verified (they were created before verification was required)
UPDATE users SET email_verified = true;

-- Mark existing organizations as onboarding completed
UPDATE organizations SET onboarding_completed = true;

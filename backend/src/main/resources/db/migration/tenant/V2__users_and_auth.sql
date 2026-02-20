-- Users table
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'WORKER',
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- Refresh token blacklist
CREATE TABLE IF NOT EXISTS refresh_token_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expired_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_blacklist_token_hash ON refresh_token_blacklist(token_hash);
CREATE INDEX idx_refresh_token_blacklist_expired_at ON refresh_token_blacklist(expired_at);

-- Default Admin User
-- Email: software@sindojan.de, Password: root1234 (BCrypt)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, active)
VALUES (
    gen_random_uuid(),
    'software@sindojan.de',
    '$2a$10$1i2Bw8xl65MuiVYtKVX53O.oETZKLJGnVNmpCDVXN.Up0aJThI9Z6',
    'System',
    'Admin',
    'ADMIN',
    true
);

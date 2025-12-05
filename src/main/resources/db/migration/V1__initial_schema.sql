-- Initial Database Schema for Mini App
-- PostgreSQL-specific migration script

-- Create schema if not exists (PostgreSQL uses 'public' by default)
CREATE SCHEMA IF NOT EXISTS public;

-- Example table with PostgreSQL best practices
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP WITH TIME ZONE
);

-- Create index for common queries
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

-- Example table for application data
CREATE TABLE IF NOT EXISTS app_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    data_key VARCHAR(255) NOT NULL,
    data_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for app_data
CREATE INDEX IF NOT EXISTS idx_app_data_user_id ON app_data(user_id);
CREATE INDEX IF NOT EXISTS idx_app_data_key ON app_data(data_key);

-- Create updated_at trigger function (PostgreSQL-specific)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to users table
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to app_data table
CREATE TRIGGER update_app_data_updated_at BEFORE UPDATE ON app_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data (optional)
-- INSERT INTO users (username, email, password_hash)
-- VALUES ('admin', 'admin@example.com', '$2a$10$dummyhash');

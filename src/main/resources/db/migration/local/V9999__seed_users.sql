-- V9999__seed_users.sql
-- LOCAL DEVELOPMENT SEED DATA ONLY
-- Seeds two test users for development and integration testing.
-- Passwords: admin123 / user123 (DEV ONLY — not for any environment besides local).

INSERT INTO users (id, username, password_hash, role, created_at)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'admin',
           '$2a$10$FYQsIGd7YMBXH57fd2vP8uLDAdPyZd3tKk86UxUCBZojr/IIgwkHW',
           'ADMIN',
           NOW()
       )
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, password_hash, role, created_at)
VALUES (
           '00000000-0000-0000-0000-000000000002',
           'testuser',
           '$2a$10$QkNee1q3pcT01.AeZV9UvOHJ1jwsRv1VdieaRlPInyDFVL42tPXo.',
           'USER',
           NOW()
       )
ON CONFLICT (username) DO NOTHING;
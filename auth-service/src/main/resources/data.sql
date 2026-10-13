-- Seed user (password: password123). The schema is managed by JPA; this script
-- only inserts the row when neither the id nor the email is present yet.
INSERT INTO users (id, email, password, role)
SELECT CAST('223e4567-e89b-12d3-a456-426614174006' AS UUID), 'testuser@test.com',
       '$2b$12$7hoRZfJrRKD2nIm2vHLs7OBETy.LWenXXMLKf99W8M4PUwO6KB7fu', 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE id = CAST('223e4567-e89b-12d3-a456-426614174006' AS UUID)
       OR email = 'testuser@test.com'
);

-- Add digital card fields to menu table
ALTER TABLE menu ADD COLUMN is_published BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE menu ADD COLUMN valid_from TIMESTAMPTZ;
ALTER TABLE menu ADD COLUMN valid_to TIMESTAMPTZ;
ALTER TABLE menu ADD COLUMN client_name VARCHAR(255);
ALTER TABLE menu ADD COLUMN client_logo_url VARCHAR(500);

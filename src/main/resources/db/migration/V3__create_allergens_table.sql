CREATE TABLE allergens (
    id INT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name_es VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    icon_url VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0
);

-- Create the user_titles table
CREATE TABLE user_titles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) UNIQUE NOT NULL
);

-- Insert default user titles
INSERT INTO user_titles (id, title) VALUES
    (1, 'Founding User'),
    (2, 'New User');

-- Create the users table with a foreign key to user_titles
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) UNIQUE NOT NULL,
    is_in_good_standing BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    entry_qrcode_token VARCHAR(255) UNIQUE NOT NULL,
    user_title_id BIGINT,
    user_stripe_member_id VARCHAR(255) UNIQUE,
    FOREIGN KEY (user_title_id) REFERENCES user_titles(id)
);

-- Insert sample users using the known IDs
INSERT INTO users ( first_name, last_name, password, phone_number, is_in_good_standing, entry_qrcode_token, user_title_id)
VALUES
    ('Alice', 'Smith', 'AlicePass321', '9876543210', FALSE, '12345', 1),
    ('Bob', 'Brown', 'BobSecure456', '4567891230', TRUE, '11111', 2);

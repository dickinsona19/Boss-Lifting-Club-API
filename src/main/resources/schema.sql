-- Create the user_titles table
CREATE TABLE user_titles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) UNIQUE NOT NULL
);

-- Insert default user titles
INSERT INTO user_titles (id, title) VALUES
    (1, 'Founding User'),
    (2, 'New User');
CREATE TABLE membership (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price VARCHAR(255) NOT NULL,
    charge_interval VARCHAR(255) NOT NULL
);

-- Modified users table with membership_id foreign key
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
    membership_id BIGINT,
    referral_code VARCHAR(255) UNIQUE NOT NULL,
    referred_by_id BIGINT,
    FOREIGN KEY (user_title_id) REFERENCES user_titles(id),
    FOREIGN KEY (membership_id) REFERENCES membership(id),
    FOREIGN KEY (referred_by_id) REFERENCES users(id)
);




-- Insert 4 different membership plans
INSERT INTO membership (name, price, charge_interval) VALUES
    ('Founder', '89.99', 'Monthly'),
    ('Standard No Contract', '109.99', 'Monthly'),
    ('Standard 6 Month', '99.99', 'Monthly'),
    ('Annual', '1,068', 'Annually');




INSERT INTO users (first_name, last_name, password, phone_number, is_in_good_standing, entry_qrcode_token, user_title_id, membership_id, referral_code, referred_by_id)
VALUES ('Andrew', 'Dickinson', '$2a$10$3aKCw124a.sRwxdI98rUMuiiSolKeK.SbSNFii4fPHgnRSSxvcZMq', '8124473167', TRUE, '123QASDFD32', 1, 3, '1243ff2ds23', NULL);

-- Insert Alice, referred by Andrew (using his referral_code '1243ff2ds')
INSERT INTO users (first_name, last_name, password, phone_number, is_in_good_standing, entry_qrcode_token, user_title_id, membership_id, referral_code, referred_by_id)
VALUES ('Alice', 'Smith', 'AlicePass321', '9876543210', FALSE, '12345', 1, 2, '1243fds',
        (SELECT id FROM users WHERE referral_code = '1243ff2ds'));

-- Insert Bob, referred by Andrew (using his referral_code '1243ff2ds')
INSERT INTO users (first_name, last_name, password, phone_number, is_in_good_standing, entry_qrcode_token, user_title_id, membership_id, referral_code, referred_by_id)
VALUES ('Bob', 'Brown', 'BobSecure456', '4567891230', TRUE, '11111', 1, 3, 'asfv335dsf',
        (SELECT id FROM users WHERE referral_code = '1243ff2ds'));
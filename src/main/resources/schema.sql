CREATE TABLE users (
    id Long PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) UNIQUE NOT NULL,
    is_in_good_standing BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    entry_barcode_token VARCHAR(255) UNIQUE NOT NULL
);

INSERT INTO users (id, first_name, last_name, password, phone_number, is_in_good_standing, entry_barcode_token)
VALUES
    (1, 'Alice', 'Smith', 'AlicePass321', '9876543210', FALSE, '12345'),
    (2, 'Bob', 'Brown', 'BobSecure456', '4567891230', TRUE, '11111');

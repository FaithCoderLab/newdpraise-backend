DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS conti_songs;
DROP TABLE IF EXISTS conti;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255),
    instrument VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

DROP TABLE IF EXISTS refresh_tokens;
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE conti (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   title VARCHAR(255),
   description TEXT,
   scheduled_at DATE NOT NULL,
   creator_id BIGINT,
   version VARCHAR(50) NOT NULL,
   original_text TEXT,
   status VARCHAR(20) NOT NULL,
   created_at TIMESTAMP NOT NULL,
   updated_at TIMESTAMP,
   FOREIGN KEY (creator_id) REFERENCES users(id)
);

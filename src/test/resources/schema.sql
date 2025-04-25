DROP TABLE IF EXISTS conti_shares;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS conti_songs;
DROP TABLE IF EXISTS songs;
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

CREATE TABLE conti (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_at DATE NOT NULL,
    creator_id BIGINT,
    version VARCHAR(20) NOT NULL,
    original_text TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    original_key VARCHAR(10),
    performance_key VARCHAR(10),
    artist VARCHAR(255),
    youtube_url VARCHAR(255),
    reference_url VARCHAR(255),
    url_type VARCHAR(20),
    duration_seconds INT,
    special_instructions TEXT,
    bpm VARCHAR(10),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE conti_songs (
     conti_id BIGINT NOT NULL,
     song_id BIGINT NOT NULL,
     position INT NOT NULL,
     PRIMARY KEY (conti_id, song_id),
     FOREIGN KEY (conti_id) REFERENCES conti(id) ON DELETE CASCADE,
     FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE conti_shares (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conti_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    shared_by BIGINT,
    permission VARCHAR(20) NOT NULL,
    accepted BOOLEAN NOT NULL DEFAULT FALSE,
    invited_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (conti_id) REFERENCES conti(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by) REFERENCES users(id),
    UNIQUE KEY uk_conti_user (conti_id, user_id)
);

CREATE TABLE IF NOT EXISTS migration_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_url VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    server_id BIGINT NOT NULL,
    cloud_id BIGINT NOT NULL
);
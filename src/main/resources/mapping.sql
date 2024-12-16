CREATE TABLE IF NOT EXISTS migration_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_url VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    server_id BIGINT NOT NULL,
    cloud_id BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_server_url_entity_type_server_id ON migration_mapping (server_url, entity_type, server_id);
CREATE INDEX IF NOT EXISTS idx_server_url_entity_type_cloud_id ON migration_mapping (server_url, entity_type, cloud_id);
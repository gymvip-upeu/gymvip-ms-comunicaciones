-- ==============================================================================
-- GYM VIP — Microservicio de Comunicaciones
-- Migración V1: Estructura inicial inmutable de communication_attempts
-- Esquema dedicado: comms (tabla de historial: flyway_schema_history_comms)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS communication_attempts (
    id UUID PRIMARY KEY,
    request_id VARCHAR(160) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(120) NOT NULL,
    recipient_masked VARCHAR(120) NOT NULL,
    client_id UUID,
    client_name VARCHAR(160),
    membership_id UUID,
    template_name VARCHAR(80),
    payload_summary VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    external_message_id VARCHAR(120),
    error_message VARCHAR(500),
    attempt_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITHOUT TIME ZONE,
    delivered_at TIMESTAMP WITHOUT TIME ZONE,
    read_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_comms_request_id ON communication_attempts (request_id);
CREATE INDEX IF NOT EXISTS idx_comms_client_id ON communication_attempts (client_id);
CREATE INDEX IF NOT EXISTS idx_comms_status ON communication_attempts (status);
CREATE INDEX IF NOT EXISTS idx_comms_external_id ON communication_attempts (external_message_id);

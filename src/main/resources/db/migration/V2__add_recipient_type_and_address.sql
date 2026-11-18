-- ==============================================================================
-- GYM VIP — Microservicio de Comunicaciones
-- Migración V2: Soporte para identificadores WhatsApp (E.164, BSUID y Usernames)
-- Cumplimiento Meta Cloud API v21/v22 y LPDP Ley 29733
-- ==============================================================================

ALTER TABLE communication_attempts
    ADD COLUMN IF NOT EXISTS recipient_type VARCHAR(30) DEFAULT 'PHONE_NUMBER',
    ADD COLUMN IF NOT EXISTS recipient_address VARCHAR(160);

-- Poblar recipient_address con recipient existente
UPDATE communication_attempts
SET recipient_address = recipient,
    recipient_type = 'PHONE_NUMBER'
WHERE recipient_address IS NULL;

CREATE INDEX IF NOT EXISTS idx_comms_recipient_type ON communication_attempts (recipient_type);

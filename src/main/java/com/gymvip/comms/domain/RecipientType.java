package com.gymvip.comms.domain;

/**
 * Tipos de direccionamiento de destinatarios para canales de comunicación en GYM VIP.
 * Soporta números telefónicos E.164 clásicos, Business-Scoped User IDs (BSUID)
 * y Usernames oficiales de WhatsApp (novedad Meta Cloud API 2026).
 */
public enum RecipientType {
    PHONE_NUMBER("Número Telefónico E.164"),
    BSUID("Business-Scoped User ID (Meta)"),
    USERNAME("Nombre de Usuario WhatsApp");

    private final String description;

    RecipientType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Infiere automáticamente el tipo de destinatario a partir del texto de entrada.
     * Si no coincide con BSUID o Username, se asume PHONE_NUMBER.
     */
    public static RecipientType inferFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return PHONE_NUMBER;
        }
        String trimmed = address.trim();
        if (trimmed.startsWith("@")) {
            return USERNAME;
        }
        if (trimmed.toLowerCase().startsWith("bsuid.") || trimmed.toLowerCase().startsWith("bsuid_") || trimmed.toLowerCase().startsWith("usr_")) {
            return BSUID;
        }
        // Si contiene letras (no es hexadecimal puro ni numérico con +), pero parece un username (sin @)
        if (trimmed.matches("^[a-zA-Z][a-zA-Z0-9._]{2,29}$") && !trimmed.matches("^\\+?[0-9]+$")) {
            return USERNAME;
        }
        return PHONE_NUMBER;
    }
}

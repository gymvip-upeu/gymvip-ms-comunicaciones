package com.gymvip.comms.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo formal de plantillas de WhatsApp aprobadas por Meta.
 * HU-17 T04.
 */
public record WhatsAppTemplate(
        String name,
        String category,
        String language,
        int expectedParameters,
        String description,
        boolean approved
) {

    public static final String TEMPLATE_EXPIRY_REMINDER = "recordatorio_vencimiento";
    public static final String TEMPLATE_OVERDUE_NOTICE = "aviso_vencido";
    public static final String TEMPLATE_RENEWAL_CONFIRMED = "renovacion_confirmada";
    public static final String TEMPLATE_WELCOME = "bienvenida_socio";

    private static final Map<String, WhatsAppTemplate> REGISTRY = Map.of(
            TEMPLATE_EXPIRY_REMINDER, new WhatsAppTemplate(
                    TEMPLATE_EXPIRY_REMINDER, "UTILITY", "es", 4,
                    "Aviso de anticipación de vencimiento: {{1}}=Nombre, {{2}}=Plan, {{3}}=Fecha Vencimiento, {{4}}=Días restantes",
                    true
            ),
            TEMPLATE_OVERDUE_NOTICE, new WhatsAppTemplate(
                    TEMPLATE_OVERDUE_NOTICE, "UTILITY", "es", 3,
                    "Aviso de membresía vencida (primeros 5 días): {{1}}=Nombre, {{2}}=Plan, {{3}}=Días de vencido",
                    true
            ),
            TEMPLATE_RENEWAL_CONFIRMED, new WhatsAppTemplate(
                    TEMPLATE_RENEWAL_CONFIRMED, "UTILITY", "es", 3,
                    "Confirmación de renovación en caja: {{1}}=Nombre, {{2}}=Plan, {{3}}=Nueva fecha fin",
                    true
            ),
            TEMPLATE_WELCOME, new WhatsAppTemplate(
                    TEMPLATE_WELCOME, "UTILITY", "es", 2,
                    "Bienvenida de nuevo socio: {{1}}=Nombre, {{2}}=Código socio MAR-XXXXXX",
                    true
            )
    );

    public static Optional<WhatsAppTemplate> findByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(name.trim().toLowerCase()));
    }

    public static boolean isApproved(String name) {
        return findByName(name).map(WhatsAppTemplate::approved).orElse(false);
    }
}

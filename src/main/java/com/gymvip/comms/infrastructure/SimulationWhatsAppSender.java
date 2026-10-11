package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SimulationWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(SimulationWhatsAppSender.class);

    @Override
    public String sendTemplateMessage(String phoneNumber, String templateName, String language, List<String> parameters) {
        String deliveryId = "sim_wsp_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SIMULATION MODE] Despachando mensaje de WhatsApp simulado:");
        log.info("  -> ID Entrega: {}", deliveryId);
        log.info("  -> Teléfono: {}", phoneNumber);
        log.info("  -> Plantilla Meta Cloud: {}", templateName);
        log.info("  -> Idioma: {}", language);
        log.info("  -> Parámetros: {}", parameters);
        log.info("  -> Estado: MENSAJE_ENVIADO_SIMULADO (Sin conexión a Meta API ni cobros por conversación)");
        return deliveryId;
    }
}

package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationStatus;
import com.gymvip.comms.domain.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador simulado explícito para entornos de desarrollo y pruebas.
 * HU-17 T03.
 * Garantiza cero costos y cero envíos a redes reales sin credenciales aprobadas.
 */
@Component("simulationWhatsAppSender")
public class SimulationWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(SimulationWhatsAppSender.class);

    @Override
    public SendResult send(WhatsAppSendRequest request) {
        String maskedPhone = CommunicationAttempt.maskRecipient(request.phoneNumber());
        String fakeWamid = "wamid.sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("[WHATSAPP SIMULADO] Despacho exitoso en entorno de pruebas:");
        log.info("  -> Destinatario enmascarado: {}", maskedPhone);
        log.info("  -> Plantilla: {}", request.templateName());
        log.info("  -> Idioma: {}", request.language());
        log.info("  -> Parámetros: {}", request.parameters());
        log.info("  -> ID Externo simulado: {}", fakeWamid);

        return SendResult.success(fakeWamid, CommunicationStatus.ENVIADO);
    }
}

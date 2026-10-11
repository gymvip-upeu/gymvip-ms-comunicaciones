package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class SimulationEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SimulationEmailSender.class);

    @Override
    public String sendEmail(String to, String subject, String template, Map<String, Object> variables) {
        String deliveryId = "sim_mail_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SIMULATION MODE] Enviando correo electrónico simulado:");
        log.info("  -> ID Entrega: {}", deliveryId);
        log.info("  -> Destinatario: {}", to);
        log.info("  -> Asunto: {}", subject);
        log.info("  -> Plantilla: {}", template);
        log.info("  -> Variables: {}", variables);
        log.info("  -> Estado: ENTREGADO_SIMULADO (Cero costo, sin envío SMTP real)");
        return deliveryId;
    }
}

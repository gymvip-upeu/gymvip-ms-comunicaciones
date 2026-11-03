package com.gymvip.comms.api;

import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationAttemptRepository;
import com.gymvip.comms.domain.CommunicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Endpoint oficial de Webhook para Meta Cloud API (WhatsApp Business).
 * HU-17 T09.
 */
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final String verifyToken;
    private final CommunicationAttemptRepository attemptRepository;

    public WhatsAppWebhookController(
            @Value("${whatsapp.webhook-verify-token:gymvip_meta_webhook_token_2026}") String verifyToken,
            CommunicationAttemptRepository attemptRepository
    ) {
        this.verifyToken = verifyToken;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Verificación del Webhook por el portal de Meta for Developers.
     * Meta envía hub.mode, hub.verify_token y hub.challenge vía GET.
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        log.info("[WEBHOOK WHATSAPP] Solicitud de verificación recibida. Mode: {}, Token: {}", mode, token != null ? "***" : "null");

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("[WEBHOOK WHATSAPP] Verificación exitosa. Retornando challenge a Meta.");
            return ResponseEntity.ok(challenge);
        }

        log.warn("[WEBHOOK WHATSAPP] Token de verificación inválido o ausente.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
    }

    /**
     * Recepción de eventos en tiempo real: cambios de estado y mensajes entrantes.
     */
    @PostMapping
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleWebhookPayload(@RequestBody Map<String, Object> payload) {
        log.info("[WEBHOOK WHATSAPP] Notificación de evento entrante recibida de Meta.");

        try {
            List<Map<String, Object>> entryList = (List<Map<String, Object>>) payload.get("entry");
            if (entryList == null || entryList.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "IGNORED", "reason", "no_entry"));
            }

            for (Map<String, Object> entry : entryList) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes == null) continue;

                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    if (value == null) continue;

                    // 1. Procesar Actualizaciones de Estado de Envío (HU-17 T05 / T09)
                    List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
                    if (statuses != null) {
                        for (Map<String, Object> st : statuses) {
                            String wamid = String.valueOf(st.get("id"));
                            String statusStr = String.valueOf(st.get("status")).toLowerCase();

                            Optional<CommunicationAttempt> opt = attemptRepository.findByExternalMessageId(wamid);
                            if (opt.isPresent()) {
                                CommunicationAttempt attempt = opt.get();
                                switch (statusStr) {
                                    case "sent" -> {
                                        attempt.setStatus(CommunicationStatus.ENVIADO);
                                        if (attempt.getSentAt() == null) attempt.setSentAt(LocalDateTime.now());
                                    }
                                    case "delivered" -> {
                                        attempt.setStatus(CommunicationStatus.ENTREGADO);
                                        attempt.setDeliveredAt(LocalDateTime.now());
                                    }
                                    case "read" -> {
                                        attempt.setStatus(CommunicationStatus.LEIDO);
                                        attempt.setReadAt(LocalDateTime.now());
                                    }
                                    case "failed" -> {
                                        attempt.setStatus(CommunicationStatus.FALLIDO);
                                        List<Map<String, Object>> errors = (List<Map<String, Object>>) st.get("errors");
                                        String err = (errors != null && !errors.isEmpty())
                                                ? String.valueOf(errors.get(0).get("title"))
                                                : "Error de entrega informado por Meta";
                                        attempt.setErrorMessage(err);
                                    }
                                    default -> attempt.setStatus(CommunicationStatus.DESCONOCIDO);
                                }
                                attemptRepository.save(attempt);
                                log.info("[WEBHOOK WHATSAPP] Estado de {} actualizado a {}", wamid, attempt.getStatus());
                            }
                        }
                    }

                    // 2. Procesar Respuestas Entrantes de Clientes (HU-17 T09 y enlace HU-16)
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    if (messages != null) {
                        for (Map<String, Object> msg : messages) {
                            String from = String.valueOf(msg.get("from"));
                            String msgId = String.valueOf(msg.get("id"));
                            Map<String, Object> textObj = (Map<String, Object>) msg.get("text");
                            String textBody = textObj != null ? String.valueOf(textObj.get("body")) : "[Mensaje no texto]";

                            log.info("[WEBHOOK WHATSAPP] Mensaje entrante de {}: ID={}, Texto: {}",
                                    CommunicationAttempt.maskRecipient(from), msgId, textBody);
                            // Los mensajes entrantes quedan listos para integración con HU-16 respuestas frecuentes
                        }
                    }
                }
            }

            return ResponseEntity.ok(Map.of("status", "PROCESSED"));

        } catch (Exception e) {
            log.error("[WEBHOOK WHATSAPP] Error al procesar carga de webhook: {}", e.getMessage(), e);
            // Meta exige 200 OK para no reintentar indefinidamente eventos con sintaxis imprevista
            return ResponseEntity.ok(Map.of("status", "ERROR_HANDLED", "error", e.getMessage()));
        }
    }
}

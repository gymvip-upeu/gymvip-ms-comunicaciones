package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationStatus;
import com.gymvip.comms.domain.RecipientType;
import com.gymvip.comms.domain.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Adaptador oficial para la API de Meta Cloud (WhatsApp Business API v20.0 - v22.0).
 * HU-17 T03.
 * Compatible con direccionamiento por teléfono E.164, BSUID y Username de WhatsApp.
 */
@Component("metaWhatsAppSender")
public class MetaWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppSender.class);

    private final String phoneNumberId;
    private final String accessToken;
    private final String apiVersion;
    private final RestClient restClient;
    private final SimulationWhatsAppSender simulationFallback;

    @Autowired
    public MetaWhatsAppSender(
            @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${whatsapp.access-token:}") String accessToken,
            @Value("${whatsapp.api-version:v20.0}") String apiVersion,
            SimulationWhatsAppSender simulationFallback
    ) {
        this(phoneNumberId, accessToken, apiVersion, simulationFallback, RestClient.builder().build());
    }

    public MetaWhatsAppSender(
            String phoneNumberId,
            String accessToken,
            String apiVersion,
            SimulationWhatsAppSender simulationFallback,
            RestClient restClient
    ) {
        this.phoneNumberId = phoneNumberId != null ? phoneNumberId.trim() : "";
        this.accessToken = accessToken != null ? accessToken.trim() : "";
        this.apiVersion = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion.trim() : "v20.0";
        this.simulationFallback = simulationFallback;
        this.restClient = restClient != null ? restClient : RestClient.builder().build();
    }

    public boolean isConfigured() {
        return phoneNumberId != null && phoneNumberId.matches("[0-9]+")
                && accessToken != null && !accessToken.isBlank()
                && !accessToken.toLowerCase(Locale.ROOT).matches(".*(placeholder|replace|example|your[_-]?token|change[_-]?me).*" )
                && apiVersion.matches("v[0-9]+[.][0-9]+");
    }

    @Override
    @SuppressWarnings("unchecked")
    public SendResult send(WhatsAppSendRequest request) {
        if (!isConfigured()) {
            String errorMsg = "Credenciales de Meta Cloud API no configuradas (WHATSAPP_PHONE_ID y/o WHATSAPP_ACCESS_TOKEN ausentes). Fallo explícito sin fallback.";
            log.error("[META WHATSAPP CRITICAL] {}", errorMsg);
            return SendResult.failure(errorMsg, false);
        }

        String rawTarget = request.recipientAddress() != null && !request.recipientAddress().isBlank()
                ? request.recipientAddress()
                : request.phoneNumber();
        RecipientType type = request.recipientType() != null ? request.recipientType() : RecipientType.inferFromAddress(rawTarget);

        String maskedTarget = CommunicationAttempt.maskRecipient(rawTarget, type);
        log.info("[META WHATSAPP] Iniciando despacho oficial ({}) a {}", type, maskedTarget);

        String url = String.format("https://graph.facebook.com/%s/%s/messages", apiVersion, phoneNumberId);

        List<Map<String, String>> bodyParameters = new ArrayList<>();
        if (request.parameters() != null) {
            for (String p : request.parameters()) {
                bodyParameters.add(Map.of("type", "text", "text", p != null ? p : ""));
            }
        }

        Map<String, Object> templateObj = new LinkedHashMap<>();
        templateObj.put("name", request.templateName());
        templateObj.put("language", Map.of("code", request.language() != null && !request.language().isBlank() ? request.language() : "es"));
        if (!bodyParameters.isEmpty()) {
            templateObj.put("components", List.of(Map.of("type", "body", "parameters", bodyParameters)));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");

        // Construir destinatario según RecipientType (E.164, BSUID o USERNAME)
        if (type == RecipientType.USERNAME) {
            payload.put("recipient_type", "username");
            payload.put("to", rawTarget.replace("@", "").trim());
        } else if (type == RecipientType.BSUID) {
            payload.put("recipient_type", "individual");
            payload.put("to", rawTarget.trim());
        } else {
            payload.put("recipient_type", "individual");
            payload.put("to", rawTarget.replace("+", "").replaceAll("[\\s\\-\\(\\)]", "").trim());
        }

        payload.put("type", "template");
        payload.put("template", templateObj);

        try {
            var response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        // El handler permite capturar el cuerpo sin lanzar excepción genérica
                    })
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var body = response.getBody();
                List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
                if (messages != null && !messages.isEmpty()) {
                    String wamid = String.valueOf(messages.get(0).get("id"));
                    log.info("[META WHATSAPP] Mensaje aceptado por Meta. WAMID: {}", wamid);
                    return SendResult.success(wamid, CommunicationStatus.ACEPTADO_PROVEEDOR);
                }
            }

            int statusCode = response.getStatusCode().value();
            boolean retryable = statusCode == 429 || (statusCode >= 500 && statusCode < 600);
            String detail = "";
            if (response.getBody() instanceof Map<?, ?> bodyMap && bodyMap.containsKey("error")) {
                Object errObj = bodyMap.get("error");
                if (errObj instanceof Map<?, ?> errMap && errMap.containsKey("message")) {
                    detail = ": " + errMap.get("message");
                }
            }
            String errorMsg = "Fallo de respuesta de Meta API (HTTP " + statusCode + ")" + detail;
            log.error("[META WHATSAPP] Error en despacho a {}: {}", maskedTarget, errorMsg);
            return SendResult.failure(errorMsg, retryable);

        } catch (Exception e) {
            log.error("[META WHATSAPP] Excepción de red al contactar Meta API: {}", e.getMessage());
            return SendResult.failure("Error de conexión con Meta Cloud API: " + e.getMessage(), true);
        }
    }
}

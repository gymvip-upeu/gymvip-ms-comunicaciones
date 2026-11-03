package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationStatus;
import com.gymvip.comms.domain.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Adaptador oficial para la API de Meta Cloud (WhatsApp Business API v20.0).
 * HU-17 T03.
 */
@Component("metaWhatsAppSender")
public class MetaWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppSender.class);

    private final String phoneNumberId;
    private final String accessToken;
    private final String apiVersion;
    private final RestClient restClient;
    private final SimulationWhatsAppSender simulationFallback;

    public MetaWhatsAppSender(
            @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${whatsapp.access-token:}") String accessToken,
            @Value("${whatsapp.api-version:v20.0}") String apiVersion,
            SimulationWhatsAppSender simulationFallback
    ) {
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.apiVersion = (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "v20.0";
        this.simulationFallback = simulationFallback;
        this.restClient = RestClient.builder().build();
    }

    public boolean isConfigured() {
        return phoneNumberId != null && !phoneNumberId.isBlank()
                && accessToken != null && !accessToken.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SendResult send(WhatsAppSendRequest request) {
        if (!isConfigured()) {
            log.warn("[META WHATSAPP] Credenciales de Meta Cloud API no configuradas. Redirigiendo a adaptador simulado.");
            return simulationFallback.send(request);
        }

        String maskedPhone = CommunicationAttempt.maskRecipient(request.phoneNumber());
        log.info("[META WHATSAPP] Iniciando despacho oficial a {}", maskedPhone);

        String url = String.format("https://graph.facebook.com/%s/%s/messages", apiVersion, phoneNumberId);

        List<Map<String, String>> bodyParameters = new ArrayList<>();
        if (request.parameters() != null) {
            for (String p : request.parameters()) {
                bodyParameters.add(Map.of("type", "text", "text", p != null ? p : ""));
            }
        }

        Map<String, Object> templateObj = new LinkedHashMap<>();
        templateObj.put("name", request.templateName());
        templateObj.put("language", Map.of("code", request.language() != null ? request.language() : "es"));
        if (!bodyParameters.isEmpty()) {
            templateObj.put("components", List.of(Map.of("type", "body", "parameters", bodyParameters)));
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", request.phoneNumber().replace("+", "").trim(),
                "type", "template",
                "template", templateObj
        );

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
            String errorMsg = "Fallo de respuesta de Meta API (HTTP " + statusCode + ")";
            log.error("[META WHATSAPP] Error en despacho a {}: {}", maskedPhone, errorMsg);
            return SendResult.failure(errorMsg, retryable);

        } catch (Exception e) {
            log.error("[META WHATSAPP] Excepción de red al contactar Meta API: {}", e.getMessage());
            return SendResult.failure("Error de conexión con Meta Cloud API: " + e.getMessage(), true);
        }
    }
}

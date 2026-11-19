package com.gymvip.comms;

import com.gymvip.comms.domain.ChannelType;
import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationAttemptRepository;
import com.gymvip.comms.domain.CommunicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WhatsAppWebhookTest {

    @Value("${whatsapp.app-secret:test-app-secret-12345}")
    private String appSecret;

    @Value("${whatsapp.webhook-verify-token:gymvip_meta_webhook_token_2026}")
    private String verifyToken;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunicationAttemptRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private String calculateHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "sha256=" + hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular firma HMAC-SHA256 para prueba", e);
        }
    }

    @Test
    @DisplayName("GET Webhook debe responder el challenge ante token de verificación válido (HU-17 T09)")
    void testWebhookVerificationSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", verifyToken)
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isOk())
                .andExpect(content().string("1158201444"));
    }

    @Test
    @DisplayName("GET Webhook debe responder 403 Forbidden ante token inválido (HU-17 T09)")
    void testWebhookVerificationForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "token_falso_invalido")
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST Webhook con firma válida debe actualizar estado a ENTREGADO según evento de Meta (HU-17 T09)")
    void testWebhookDeliveredStatusUpdate() throws Exception {
        String wamid = "wamid.HBgLMjAyNjExMDM=";
        CommunicationAttempt attempt = new CommunicationAttempt(
                UUID.randomUUID(), "REQ-WSP-123", ChannelType.WHATSAPP, "+51987654321",
                UUID.randomUUID(), "Ana Quispe", UUID.randomUUID(), "recordatorio_vencimiento", "summary"
        );
        attempt.setStatus(CommunicationStatus.ENVIADO);
        attempt.setExternalMessageId(wamid);
        repository.save(attempt);

        String metaPayload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "statuses": [{
                      "id": "%s",
                      "status": "delivered",
                      "timestamp": "1730600000"
                    }]
                  }
                }]
              }]
            }
        """.formatted(wamid);

        String validSignature = calculateHmacSha256(metaPayload, appSecret);

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", validSignature)
                        .content(metaPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));

        CommunicationAttempt updated = repository.findByExternalMessageId(wamid).orElseThrow();
        assertEquals(CommunicationStatus.ENTREGADO, updated.getStatus());
    }

    @Test
    @DisplayName("POST Webhook sin firma debe responder 401 Unauthorized cuando appSecret está configurado (HU-17 T09)")
    void testWebhookPostUnauthorizedWithoutSignature() throws Exception {
        String dummyPayload = """
            {
              "entry": []
            }
        """;

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dummyPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Firma criptográfica inválida")));
    }

    @Test
    @DisplayName("POST Webhook con firma HMAC inválida debe responder 401 Unauthorized (HU-17 T09)")
    void testWebhookPostUnauthorizedWithInvalidSignature() throws Exception {
        String dummyPayload = """
            {
              "entry": []
            }
        """;

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=invalid000000000000000000000000000000000000000000000000000000000000")
                        .content(dummyPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Firma criptográfica inválida")));
    }
}

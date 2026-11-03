package com.gymvip.comms;

import com.gymvip.comms.domain.ChannelType;
import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationAttemptRepository;
import com.gymvip.comms.domain.CommunicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WhatsAppWebhookTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunicationAttemptRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("GET Webhook debe responder el challenge ante token de verificación válido (HU-17 T09)")
    void testWebhookVerificationSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "gymvip_meta_webhook_token_2026")
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
    @DisplayName("POST Webhook debe actualizar estado a ENTREGADO según evento de Meta (HU-17 T09)")
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

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metaPayload))
                .andExpect(status().isOk());

        CommunicationAttempt updated = repository.findByExternalMessageId(wamid).orElseThrow();
        assertEquals(CommunicationStatus.ENTREGADO, updated.getStatus());
    }
}

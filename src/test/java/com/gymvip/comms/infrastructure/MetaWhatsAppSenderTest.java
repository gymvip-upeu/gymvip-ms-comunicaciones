package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.CommunicationStatus;
import com.gymvip.comms.domain.WhatsAppSender;
import com.gymvip.comms.domain.WhatsAppTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MetaWhatsAppSenderTest {

    private final SimulationWhatsAppSender simulationSender = new SimulationWhatsAppSender();

    @Test
    @DisplayName("Debe fallar explícitamente sin fallback a simulación si no hay credenciales")
    void testFailsExplicitlyWithoutCredentials() {
        MetaWhatsAppSender unconfigured = new MetaWhatsAppSender("", "", "v20.0", simulationSender);
        assertFalse(unconfigured.isConfigured());

        var req = new WhatsAppSender.WhatsAppSendRequest(
                "+51987654321",
                WhatsAppTemplate.TEMPLATE_EXPIRY_REMINDER,
                "es",
                List.of("Juan", "Plan VIP", "2026-12-31", "5")
        );

        var result = unconfigured.send(req);

        assertNotNull(result);
        assertFalse(result.success());
        assertFalse(result.retryable());
        assertTrue(result.errorMessage().contains("Credenciales de Meta Cloud API no configuradas"));
    }

    @Test
    @DisplayName("Debe procesar exitosamente respuesta 200 OK de Meta con WAMID")
    void testSuccessfulSendWith200() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        MetaWhatsAppSender sender = new MetaWhatsAppSender("123456", "token_abc", "v20.0", simulationSender, restClient);
        assertTrue(sender.isConfigured());

        String metaResponseJson = """
                {
                  "messaging_product": "whatsapp",
                  "contacts": [{"input": "51987654321", "wa_id": "51987654321"}],
                  "messages": [{"id": "wamid.HBgNNTE5ODc2NTQzMjEVAgASGB..."}]
                }
                """;

        server.expect(requestTo("https://graph.facebook.com/v20.0/123456/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token_abc"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.template.name").value("hello_world"))
                .andExpect(jsonPath("$.template.language.code").value("en_US"))
                .andRespond(withSuccess(metaResponseJson, MediaType.APPLICATION_JSON));

        var req = new WhatsAppSender.WhatsAppSendRequest(
                "+51987654321",
                WhatsAppTemplate.TEMPLATE_HELLO_WORLD,
                "en_US",
                List.of()
        );

        var result = sender.send(req);

        assertTrue(result.success());
        assertEquals("wamid.HBgNNTE5ODc2NTQzMjEVAgASGB...", result.externalId());
        assertEquals(CommunicationStatus.ACEPTADO_PROVEEDOR, result.status());
        server.verify();
    }

    @Test
    @DisplayName("Debe registrar fallo no reintentable ante HTTP 400 Bad Request de Meta")
    void testNonRetryableFailureOnHttp400() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        MetaWhatsAppSender sender = new MetaWhatsAppSender("123456", "token_abc", "v20.0", simulationSender, restClient);

        String metaErrorJson = """
                {
                  "error": {
                    "message": "(#100) Param template['components'] is invalid",
                    "type": "OAuthException",
                    "code": 100
                  }
                }
                """;

        server.expect(requestTo("https://graph.facebook.com/v20.0/123456/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body(metaErrorJson).contentType(MediaType.APPLICATION_JSON));

        var req = new WhatsAppSender.WhatsAppSendRequest(
                "+51987654321",
                WhatsAppTemplate.TEMPLATE_EXPIRY_REMINDER,
                "es",
                List.of()
        );

        var result = sender.send(req);

        assertFalse(result.success());
        assertFalse(result.retryable(), "Un error 400 no debe ser reintentable");
        assertTrue(result.errorMessage().contains("400"));
        server.verify();
    }

    @Test
    @DisplayName("Debe marcar como reintentable ante HTTP 429 Rate Limit o 500 Server Error")
    void testRetryableFailureOnHttp429And500() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        MetaWhatsAppSender sender = new MetaWhatsAppSender("123456", "token_abc", "v20.0", simulationSender, restClient);

        server.expect(requestTo("https://graph.facebook.com/v20.0/123456/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"message\":\"Rate limit hit\"}}").contentType(MediaType.APPLICATION_JSON));

        var req = new WhatsAppSender.WhatsAppSendRequest(
                "+51987654321",
                WhatsAppTemplate.TEMPLATE_EXPIRY_REMINDER,
                "es",
                List.of()
        );

        var result = sender.send(req);

        assertFalse(result.success());
        assertTrue(result.retryable(), "Un error 429 DEBE ser reintentable");
        assertTrue(result.errorMessage().contains("429"));
        server.verify();
    }
}

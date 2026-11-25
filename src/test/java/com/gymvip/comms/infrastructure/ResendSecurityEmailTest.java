package com.gymvip.comms.infrastructure;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResendSecurityEmailTest {

    @Test
    void activationIncludesRegisteredEmailTemporaryPasswordAndSafeButton() {
        var sender = new ResendEmailSender("fixture", "VIP CENTER FIT <onboarding@resend.dev>");
        String html = sender.renderContent("account_activation", Map.of(
                "username", "qa@example.test",
                "temporaryPassword", "TemporalQa123!",
                "link", "http://127.0.0.1:5173/activar-cuenta#token=qa&test=1"
        ));
        assertTrue(html.contains("Activar mi cuenta"));
        assertTrue(html.contains("qa@example.test"));
        assertTrue(html.contains("TemporalQa123!"));
        assertTrue(html.contains("24 horas"));
        assertTrue(html.contains("&amp;test=1"));
    }

    @Test
    void passwordChangedNeverIncludesPasswordOrTokenEvenIfVariablesContainThem() {
        var sender = new ResendEmailSender("fixture", "test@example.test");
        String html = sender.renderContent("password_changed", Map.of(
                "newPassword", "never-expose-this",
                "token", "private-token",
                "temporaryPassword", "another-secret"
        ));
        assertFalse(html.contains("never-expose-this"));
        assertFalse(html.contains("private-token"));
        assertFalse(html.contains("another-secret"));
        assertTrue(html.contains("sesiones anteriores se cerraron"));
    }

    @Test
    void executableLinksAreRejected() {
        var sender = new ResendEmailSender("fixture", "test@example.test");
        assertThrows(IllegalArgumentException.class, () ->
                sender.renderContent("password_reset", Map.of("link", "javascript:alert(1)")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void providerErrorsAreSanitizedAndSuccessfulResponsesRequireAnIdentifier() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(403);
        var sender = new ResendEmailSender("fixture-private-key", "test@example.test", client);
        var error = assertThrows(IllegalStateException.class, () ->
                sender.sendEmail("qa@example.test", "Test", "password_changed", Map.of()));
        assertFalse(error.getMessage().contains("fixture-private-key"));

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{}");
        assertThrows(IllegalStateException.class, () ->
                sender.sendEmail("qa@example.test", "Test", "password_changed", Map.of()));

        when(response.body()).thenReturn("{\"id\":\"fixture-provider-id\"}");
        assertEquals("fixture-provider-id", sender.sendEmail(
                "qa@example.test", "Test", "password_changed", Map.of("idempotencyKey", "mail-qa-1")));
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(3)).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("mail-qa-1", request.getValue().headers().firstValue("Idempotency-Key").orElseThrow());
    }

    @Test
    void incompleteProductionConfigurationNeverSilentlySimulates() {
        var config = new EmailConfig();
        var sender = config.primaryEmailSender(
                "production",
                "resend",
                new ResendEmailSender("", "test@example.test"),
                new SimulationEmailSender()
        );
        assertThrows(IllegalStateException.class, () ->
                sender.sendEmail("qa@example.test", "Test", "password_changed", Map.of()));
    }
}

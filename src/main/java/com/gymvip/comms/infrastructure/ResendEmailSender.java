package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador de envío transaccional de correos mediante Resend API.
 * HU-40.
 */
@Component("resendEmailSender")
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final String apiKey;
    private final String fromEmail;
    private final HttpClient httpClient;

    public ResendEmailSender(
            @Value("${email.resend.api-key:}") String apiKey,
            @Value("${email.from:notificaciones@gymvip.pe}") String fromEmail
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "notificaciones@gymvip.pe";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public String sendEmail(String to, String subject, String template, Map<String, Object> variables) {
        if (!isConfigured()) {
            throw new IllegalStateException("Resend API Key no configurada. Configure RESEND_API_KEY en las variables de entorno.");
        }

        try {
            String htmlContent = renderContent(template, variables);
            String payload = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    escapeJson(fromEmail),
                    escapeJson(to),
                    escapeJson(subject),
                    escapeJson(htmlContent)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                Matcher matcher = ID_PATTERN.matcher(body);
                String id = matcher.find() ? matcher.group(1) : "resend_" + System.currentTimeMillis();
                log.info("[RESEND] Correo despachado con éxito. ID: {}, Destinatario: {}", id, maskEmail(to));
                return id;
            } else {
                log.error("[RESEND ERROR] Código {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Error en proveedor Resend (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            log.error("[RESEND EXCEPTION] Error enviando correo a {}: {}", maskEmail(to), e.getMessage());
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("Fallo en despacho de correo Resend: " + e.getMessage(), e);
        }
    }

    private String renderContent(String template, Map<String, Object> variables) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>");
        sb.append("<h2 style='color: #D4AF37;'>GYM VIP</h2>");
        sb.append("<p>Notificación del sistema:</p>");
        if (template != null && !template.isBlank()) {
            sb.append("<p><strong>Tipo:</strong> ").append(escapeHtml(template)).append("</p>");
        }
        if (variables != null && !variables.isEmpty()) {
            sb.append("<ul>");
            variables.forEach((k, v) -> sb.append("<li><strong>").append(escapeHtml(k)).append(":</strong> ").append(escapeHtml(String.valueOf(v))).append("</li>"));
            sb.append("</ul>");
        }
        sb.append("<p style='font-size: 12px; color: #888;'>Este es un mensaje automático de GYM VIP. Por favor no responda directamente a este correo.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String maskEmail(String email) {
        if (email == null) return "-";
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(at);
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}

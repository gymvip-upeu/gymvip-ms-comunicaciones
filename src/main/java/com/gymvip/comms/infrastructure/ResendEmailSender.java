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

    @org.springframework.beans.factory.annotation.Autowired
    public ResendEmailSender(
            @Value("${email.resend.api-key:}") String apiKey,
            @Value("${email.from:notificaciones@gymvip.pe}") String fromEmail
    ) {
        this(apiKey,fromEmail,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }
    ResendEmailSender(String apiKey,String fromEmail,HttpClient httpClient) {
        this.apiKey=apiKey!=null?apiKey.trim():"";this.fromEmail=fromEmail!=null?fromEmail.trim():"";this.httpClient=httpClient;
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

            var builder = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            Object idempotency=variables!=null?variables.get("idempotencyKey"):null;
            if(idempotency instanceof String key && key.matches("[A-Za-z0-9_-]{1,100}")) builder.header("Idempotency-Key",key);
            HttpRequest request=builder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                Matcher matcher = ID_PATTERN.matcher(body);
                if(!matcher.find()) throw new IllegalStateException("Respuesta de correo incompleta.");
                String id = matcher.group(1);
                log.info("[RESEND] Correo despachado con éxito. ID: {}, Destinatario: {}", id, maskEmail(to));
                return id;
            } else {
                log.warn("[RESEND] Envío rechazado, HTTP {}", response.statusCode());
                throw new IllegalStateException("El proveedor de correo rechazó el envío (HTTP " + response.statusCode() + ").");
            }
        } catch (Exception e) {
            log.warn("[RESEND] No se pudo completar el envío ({})",e.getClass().getSimpleName());
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo completar el envío del correo.");
        }
    }

    String renderContent(String template, Map<String, Object> variables) {
        if(template!=null && java.util.Set.of("account_activation","password_reset","password_changed").contains(template))
            return renderSecurity(template,variables!=null?variables:Map.of());
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

    private String renderSecurity(String template,Map<String,Object> variables) {
        String brand="<span translate=\"no\" class=\"notranslate\">VIP CENTER FIT</span>";
        String heading=template.equals("account_activation")?"Bienvenido a "+brand:template.equals("password_reset")?"Restablece tu contraseña":"Tu contraseña fue actualizada";
        StringBuilder html=new StringBuilder("<!doctype html><html lang=\"es\" translate=\"no\"><head><meta charset=\"UTF-8\"><meta name=\"google\" content=\"notranslate\"></head><body lang=\"es\"><div style=\"font-family:Arial,sans-serif;background:#101720;color:#fff;padding:32px;max-width:600px;margin:auto;border-radius:16px\"><p style=\"color:#e8c66a;letter-spacing:2px\">").append(brand).append("</p><h1 style=\"font-size:26px\">").append(heading).append("</h1>");
        if(template.equals("account_activation")) {
            html.append("<p>Recepción creó tu acceso al gimnasio. Activa tu cuenta y crea una contraseña propia.</p><p>Usuario de acceso: <strong>")
                .append(escapeHtml(String.valueOf(variables.get("username")))).append("</strong></p><p>Contraseña temporal: <strong>")
                .append(escapeHtml(String.valueOf(variables.get("temporaryPassword")))).append("</strong></p>")
                .append("<p>La contraseña temporal y esta invitación vencen en 24 horas. También puedes ingresar con esos datos en la app: se te pedirá crear tu nueva contraseña antes de continuar.</p>");
        } else if(template.equals("password_reset")) {
            html.append("<p>Recibimos una solicitud para restablecer tu contraseña. El enlace vence en 15 minutos y solo puede utilizarse una vez.</p>");
        } else html.append("<p>La contraseña de tu cuenta fue actualizada. Tus sesiones anteriores se cerraron. Si no realizaste este cambio, contacta a Recepción y recupera el acceso desde la app.</p>");
        if(!template.equals("password_changed")) {
            String link=String.valueOf(variables.get("link"));
            var uri=URI.create(link);
            if(!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
                throw new IllegalArgumentException("Enlace de seguridad no válido.");
            html.append("<p style=\"margin:28px 0\"><a style=\"background:#e8c66a;color:#101720;padding:14px 22px;border-radius:8px;text-decoration:none;display:inline-block;font-weight:bold\" href=\"")
                .append(escapeHtml(link)).append("\">").append(template.equals("account_activation")?"Activar mi cuenta":"Restablecer contraseña").append("</a></p>")
                .append("<p>Si no solicitaste este acceso, puedes ignorar este mensaje. No compartas este enlace ni tus credenciales.</p>");
        }
        return html.append("<hr style=\"border:0;border-top:1px solid #354252\"><p style=\"font-size:12px;color:#cad2df\">").append(brand).append(" · Seguridad de tu cuenta</p></div></body></html>").toString();
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

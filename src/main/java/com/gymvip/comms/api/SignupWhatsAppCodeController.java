package com.gymvip.comms.api;

import com.gymvip.comms.domain.CommunicationStatus;
import com.gymvip.comms.domain.RecipientType;
import com.gymvip.comms.domain.WhatsAppSender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages/whatsapp/signup-code")
public class SignupWhatsAppCodeController {
    private final WhatsAppSender sender;
    private final String template;
    private final String language;
    private final String mode;

    public SignupWhatsAppCodeController(WhatsAppSender sender,
            @Value("${WHATSAPP_SIGNUP_TEMPLATE:}") String template,
            @Value("${WHATSAPP_SIGNUP_LANGUAGE:es}") String language,
            @Value("${comms.mode:simulation}") String mode) {
        this.sender = sender;
        this.template = template;
        this.language = language;
        this.mode = mode;
    }

    public record CodeRequest(
            @NotBlank @Pattern(regexp = "[+]519[0-9]{8}") String phone,
            @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {
        @Override public String toString() { return "CodeRequest[contenido omitido]"; }
    }

    @PostMapping
    public Map<String, String> send(@Valid @RequestBody CodeRequest request) {
        if (!"production".equalsIgnoreCase(mode) || !template.matches("[a-z0-9_]{1,512}")
                || !language.matches("[a-z]{2,3}(_[A-Z]{2})?")) throw unavailable();

        var result = sender.send(new WhatsAppSender.WhatsAppSendRequest(request.phone(), template, language,
                List.of(request.code()), RecipientType.PHONE_NUMBER, request.phone(), true));
        if (!result.success() || result.status() != CommunicationStatus.ACEPTADO_PROVEEDOR) throw unavailable();
        return Map.of("status", result.status().name());
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Canal de autenticación de WhatsApp no disponible.");
    }
}

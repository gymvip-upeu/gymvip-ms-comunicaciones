package com.gymvip.comms.api;

import com.gymvip.comms.domain.EmailSender;
import com.gymvip.comms.domain.WhatsAppSender;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
public class CommsController {

    private final EmailSender emailSender;
    private final WhatsAppSender whatsAppSender;

    public CommsController(EmailSender emailSender, WhatsAppSender whatsAppSender) {
        this.emailSender = emailSender;
        this.whatsAppSender = whatsAppSender;
    }

    public record SendEmailRequest(
            @NotBlank(message = "Destinatario obligatorio") String to,
            @NotBlank(message = "Asunto obligatorio") String subject,
            String template,
            Map<String, Object> variables
    ) {}

    public record SendWhatsAppRequest(
            @NotBlank(message = "Teléfono obligatorio") String phoneNumber,
            @NotBlank(message = "Plantilla obligatoria") String templateName,
            String language,
            List<String> parameters
    ) {}

    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@RequestBody SendEmailRequest request) {
        String deliveryId = emailSender.sendEmail(request.to(), request.subject(), request.template(), request.variables());
        return ResponseEntity.ok(Map.of(
                "status", "SIMULATED_DISPATCHED",
                "deliveryId", deliveryId,
                "recipient", request.to()
        ));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<?> sendWhatsApp(@RequestBody SendWhatsAppRequest request) {
        String deliveryId = whatsAppSender.sendTemplateMessage(request.phoneNumber(), request.templateName(), request.language() != null ? request.language() : "es", request.parameters());
        return ResponseEntity.ok(Map.of(
                "status", "SIMULATED_DISPATCHED",
                "deliveryId", deliveryId,
                "recipient", request.phoneNumber()
        ));
    }
}

package com.gymvip.comms.api;

import com.gymvip.comms.domain.EmailSender;
import com.gymvip.comms.domain.WhatsAppService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
public class CommsController {

    private final EmailSender emailSender;
    private final WhatsAppService whatsAppService;

    public CommsController(EmailSender emailSender, WhatsAppService whatsAppService) {
        this.emailSender = emailSender;
        this.whatsAppService = whatsAppService;
    }

    public record SendEmailRequest(
            @NotBlank(message = "Destinatario obligatorio") String to,
            @NotBlank(message = "Asunto obligatorio") String subject,
            String template,
            Map<String, Object> variables
    ) {}

    public record SendWhatsAppMessageRequest(
            String requestId,
            @NotBlank(message = "Teléfono obligatorio") String phoneNumber,
            @NotBlank(message = "Plantilla obligatoria") String templateName,
            String language,
            List<String> parameters,
            Boolean consentWhatsapp,
            UUID clientId,
            String clientName,
            UUID membershipId
    ) {}

    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        String deliveryId = emailSender.sendEmail(request.to(), request.subject(), request.template(), request.variables());
        return ResponseEntity.ok(Map.of(
                "status", "SIMULATED_DISPATCHED",
                "deliveryId", deliveryId,
                "recipient", request.to()
        ));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<?> sendWhatsApp(@Valid @RequestBody SendWhatsAppMessageRequest request) {
        try {
            var serviceRequest = new WhatsAppService.SendWhatsAppNotificationRequest(
                    request.requestId(),
                    request.phoneNumber(),
                    request.templateName(),
                    request.language() != null ? request.language() : "es",
                    request.parameters(),
                    request.consentWhatsapp() == null || request.consentWhatsapp(),
                    request.clientId(),
                    request.clientName(),
                    request.membershipId()
            );

            WhatsAppService.CommunicationResponse response = whatsAppService.processSendRequest(serviceRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", "Bad Request",
                    "message", e.getMessage()
            ));
        }
    }
}

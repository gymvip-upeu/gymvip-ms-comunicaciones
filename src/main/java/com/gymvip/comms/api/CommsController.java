package com.gymvip.comms.api;

import com.gymvip.comms.domain.EmailSender;
import com.gymvip.comms.domain.RecipientType;
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
            @NotBlank(message = "Destinatario obligatorio") @jakarta.validation.constraints.Email String to,
            @NotBlank(message = "Asunto obligatorio") String subject,
            String template,
            Map<String, Object> variables
    ) {}

    public record SendWhatsAppMessageRequest(
            String requestId,
            String phoneNumber,
            @NotBlank(message = "Plantilla obligatoria") String templateName,
            String language,
            List<String> parameters,
            Boolean consentWhatsapp,
            UUID clientId,
            String clientName,
            UUID membershipId,
            RecipientType recipientType,
            String recipientAddress
    ) {
        public SendWhatsAppMessageRequest(
                String requestId,
                String phoneNumber,
                String templateName,
                String language,
                List<String> parameters,
                Boolean consentWhatsapp,
                UUID clientId,
                String clientName,
                UUID membershipId
        ) {
            this(requestId, phoneNumber, templateName, language, parameters, consentWhatsapp,
                    clientId, clientName, membershipId, null, null);
        }
    }

    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        String deliveryId = emailSender.sendEmail(request.to(), request.subject(), request.template(), request.variables());
        return ResponseEntity.ok(Map.of(
                "status", deliveryId.startsWith("sim_mail_") ? "SIMULATED_DISPATCHED" : "ACEPTADO_PROVEEDOR",
                "deliveryId", deliveryId,
                "recipient", request.to()
        ));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<?> sendWhatsApp(@Valid @RequestBody SendWhatsAppMessageRequest request) {
        try {
            String targetAddress = request.recipientAddress() != null && !request.recipientAddress().isBlank()
                    ? request.recipientAddress().trim()
                    : (request.phoneNumber() != null ? request.phoneNumber().trim() : null);

            if (targetAddress == null || targetAddress.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", "Debe indicarse un número de teléfono, BSUID o Username de WhatsApp."
                ));
            }

            RecipientType rType = request.recipientType() != null
                    ? request.recipientType()
                    : RecipientType.inferFromAddress(targetAddress);

            var serviceRequest = new WhatsAppService.SendWhatsAppNotificationRequest(
                    request.requestId(),
                    targetAddress,
                    request.templateName(),
                    request.language() != null ? request.language() : "es",
                    request.parameters(),
                    request.consentWhatsapp() == null || request.consentWhatsapp(),
                    request.clientId(),
                    request.clientName(),
                    request.membershipId(),
                    rType,
                    targetAddress
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

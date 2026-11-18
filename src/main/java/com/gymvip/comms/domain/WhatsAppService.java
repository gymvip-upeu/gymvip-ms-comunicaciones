package com.gymvip.comms.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Servicio de negocio para el procesamiento, validación y trazabilidad de WhatsApp.
 * HU-17 T05, T06, T07, T08.
 * Compatible con direccionamiento E.164, BSUID y Usernames (Meta WhatsApp 2026).
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final Pattern PHONE_DIGITS_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^@?[a-zA-Z0-9._]{3,30}$");
    private static final Pattern BSUID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{6,128}$");

    private final WhatsAppSender whatsAppSender;
    private final CommunicationAttemptRepository attemptRepository;

    public WhatsAppService(WhatsAppSender whatsAppSender, CommunicationAttemptRepository attemptRepository) {
        this.whatsAppSender = whatsAppSender;
        this.attemptRepository = attemptRepository;
    }

    public record SendWhatsAppNotificationRequest(
            String requestId,
            String phoneNumber,
            String templateName,
            String language,
            List<String> parameters,
            boolean consentWhatsapp,
            UUID clientId,
            String clientName,
            UUID membershipId,
            RecipientType recipientType,
            String recipientAddress
    ) {
        public SendWhatsAppNotificationRequest(
                String requestId,
                String phoneNumber,
                String templateName,
                String language,
                List<String> parameters,
                boolean consentWhatsapp,
                UUID clientId,
                String clientName,
                UUID membershipId
        ) {
            this(requestId, phoneNumber, templateName, language, parameters, consentWhatsapp, clientId, clientName, membershipId,
                    RecipientType.inferFromAddress(phoneNumber), phoneNumber);
        }
    }

    public record CommunicationResponse(
            UUID id,
            String requestId,
            String channel,
            String recipientMasked,
            String clientName,
            String templateName,
            CommunicationStatus status,
            String externalMessageId,
            String errorMessage,
            int attemptCount,
            LocalDateTime createdAt,
            LocalDateTime sentAt
    ) {
        public static CommunicationResponse from(CommunicationAttempt a) {
            return new CommunicationResponse(
                    a.getId(),
                    a.getRequestId(),
                    a.getChannel().name(),
                    a.getRecipientMasked(),
                    a.getClientName(),
                    a.getTemplateName(),
                    a.getStatus(),
                    a.getExternalMessageId(),
                    a.getErrorMessage(),
                    a.getAttemptCount(),
                    a.getCreatedAt(),
                    a.getSentAt()
            );
        }
    }

    /**
     * Normaliza un número telefónico a estándar E.164.
     * Soporta números peruanos directos (ej. 987654321 -> +51987654321).
     */
    public String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        if (cleaned.length() == 9 && cleaned.startsWith("9")) {
            return "+51" + cleaned;
        }
        if (!cleaned.startsWith("+") && cleaned.length() >= 8) {
            return "+" + cleaned;
        }
        return cleaned;
    }

    @Transactional
    public CommunicationResponse processSendRequest(SendWhatsAppNotificationRequest request) {
        // 1. Control de Idempotencia (HU-14 T07 y HU-17 T08)
        String reqId = request.requestId() != null && !request.requestId().isBlank()
                ? request.requestId().trim()
                : "REQ-" + UUID.randomUUID();

        Optional<CommunicationAttempt> existing = attemptRepository.findByRequestId(reqId);
        if (existing.isPresent()) {
            CommunicationAttempt prev = existing.get();
            log.info("[WHATSAPP IDEMPOTENCIA] Solicitud {} ya procesada con estado: {}", reqId, prev.getStatus());
            return CommunicationResponse.from(prev);
        }

        // 2. Determinación y Validación de Destinatario (HU-17 T06, BSUID y Usernames Meta 2026)
        String rawAddress = request.recipientAddress() != null && !request.recipientAddress().isBlank()
                ? request.recipientAddress().trim()
                : (request.phoneNumber() != null ? request.phoneNumber().trim() : null);

        if (rawAddress == null || rawAddress.isBlank()) {
            throw new IllegalArgumentException("Debe especificarse un destinatario válido para WhatsApp.");
        }

        RecipientType recipientType = request.recipientType() != null
                ? request.recipientType()
                : RecipientType.inferFromAddress(rawAddress);

        String validatedAddress;
        if (recipientType == RecipientType.USERNAME) {
            if (!USERNAME_PATTERN.matcher(rawAddress).matches()) {
                throw new IllegalArgumentException("Nombre de usuario de WhatsApp inválido: " + rawAddress);
            }
            validatedAddress = rawAddress.startsWith("@") ? rawAddress : "@" + rawAddress;
        } else if (recipientType == RecipientType.BSUID) {
            if (!BSUID_PATTERN.matcher(rawAddress).matches()) {
                throw new IllegalArgumentException("Identificador BSUID inválido para WhatsApp: " + rawAddress);
            }
            validatedAddress = rawAddress;
        } else {
            // PHONE_NUMBER E.164
            String normalizedPhone = normalizePhoneNumber(rawAddress);
            if (normalizedPhone == null || !PHONE_DIGITS_PATTERN.matcher(normalizedPhone).matches()) {
                throw new IllegalArgumentException("Número de teléfono inválido para WhatsApp. Debe seguir formato E.164 (ej. +519XXXXXXXX).");
            }
            validatedAddress = normalizedPhone;
        }

        // 3. Validación de Consentimiento Específico de Canal (HU-17 T06 y LPDP Ley 29733)
        if (!request.consentWhatsapp()) {
            throw new IllegalStateException("El miembro no ha otorgado consentimiento explícito para recibir avisos vía WhatsApp.");
        }

        // 4. Validación de Plantilla Aprobada (HU-17 T04)
        String tplName = request.templateName() != null ? request.templateName().trim().toLowerCase() : "";
        if (!WhatsAppTemplate.isApproved(tplName)) {
            throw new IllegalArgumentException("La plantilla '" + tplName + "' no está aprobada en Meta Cloud API o no está autorizada.");
        }

        WhatsAppTemplate templateInfo = WhatsAppTemplate.findByName(tplName).orElseThrow();
        List<String> params = request.parameters() != null ? request.parameters() : List.of();
        if (params.size() < templateInfo.expectedParameters()) {
            throw new IllegalArgumentException(String.format("La plantilla '%s' requiere al menos %d parámetros. Recibidos: %d",
                    tplName, templateInfo.expectedParameters(), params.size()));
        }

        // 5. Creación de Registro de Auditoría (HU-17 T07)
        String payloadSummary = String.format("Plantilla: %s, Tipo: %s, Parámetros: %s", tplName, recipientType, params);
        CommunicationAttempt attempt = new CommunicationAttempt(
                UUID.randomUUID(),
                reqId,
                ChannelType.WHATSAPP,
                recipientType,
                validatedAddress,
                validatedAddress,
                request.clientId(),
                request.clientName(),
                request.membershipId(),
                tplName,
                payloadSummary
        );
        attempt = attemptRepository.save(attempt);

        // 6. Despacho al Proveedor (HU-17 T05)
        WhatsAppSender.WhatsAppSendRequest sendReq = new WhatsAppSender.WhatsAppSendRequest(
                validatedAddress,
                tplName,
                request.language() != null ? request.language() : templateInfo.language(),
                params,
                recipientType,
                validatedAddress
        );

        WhatsAppSender.SendResult result = whatsAppSender.send(sendReq);

        if (result.success()) {
            attempt.setStatus(result.status());
            attempt.setExternalMessageId(result.externalId());
            attempt.setSentAt(LocalDateTime.now());
        } else {
            attempt.setStatus(CommunicationStatus.FALLIDO);
            attempt.setErrorMessage(result.errorMessage());
        }

        attempt = attemptRepository.save(attempt);
        return CommunicationResponse.from(attempt);
    }

    @Transactional
    public CommunicationResponse retryAttempt(UUID attemptId) {
        CommunicationAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Intento de comunicación no encontrado"));

        if (attempt.getStatus() != CommunicationStatus.FALLIDO) {
            throw new IllegalStateException("Solo se permite reintentar comunicaciones en estado FALLIDO. Estado actual: " + attempt.getStatus());
        }

        if (attempt.getAttemptCount() >= 3) {
            throw new IllegalStateException("Límite máximo de reintentos alcanzado (3 intentos). Requiere revisión manual.");
        }

        attempt.incrementAttemptCount();
        attempt.setStatus(CommunicationStatus.SOLICITADO);

        String target = attempt.getRecipientAddress() != null ? attempt.getRecipientAddress() : attempt.getRecipient();
        RecipientType type = attempt.getRecipientType() != null ? attempt.getRecipientType() : RecipientType.inferFromAddress(target);

        WhatsAppSender.WhatsAppSendRequest sendReq = new WhatsAppSender.WhatsAppSendRequest(
                target,
                attempt.getTemplateName(),
                "es",
                List.of(),
                type,
                target
        );

        WhatsAppSender.SendResult result = whatsAppSender.send(sendReq);

        if (result.success()) {
            attempt.setStatus(result.status());
            attempt.setExternalMessageId(result.externalId());
            attempt.setSentAt(LocalDateTime.now());
            attempt.setErrorMessage(null);
        } else {
            attempt.setStatus(CommunicationStatus.FALLIDO);
            attempt.setErrorMessage(result.errorMessage());
        }

        return CommunicationResponse.from(attemptRepository.save(attempt));
    }
}

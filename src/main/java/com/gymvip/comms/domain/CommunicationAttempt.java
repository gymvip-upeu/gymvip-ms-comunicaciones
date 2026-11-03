package com.gymvip.comms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro inmutable de auditoría e intentos de comunicación.
 * HU-17 T07 y HU-18 T05.
 */
@Entity
@Table(name = "communication_attempts", indexes = {
        @Index(name = "idx_comms_request_id", columnList = "request_id", unique = true),
        @Index(name = "idx_comms_client_id", columnList = "client_id"),
        @Index(name = "idx_comms_status", columnList = "status"),
        @Index(name = "idx_comms_external_id", columnList = "external_message_id")
})
public class CommunicationAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true, length = 160)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private ChannelType channel;

    @Column(name = "recipient", nullable = false, length = 120)
    private String recipient;

    @Column(name = "recipient_masked", nullable = false, length = 120)
    private String recipientMasked;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "client_name", length = 160)
    private String clientName;

    @Column(name = "membership_id")
    private UUID membershipId;

    @Column(name = "template_name", length = 80)
    private String templateName;

    @Column(name = "payload_summary", length = 1000)
    private String payloadSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CommunicationStatus status;

    @Column(name = "external_message_id", length = 120)
    private String externalMessageId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public CommunicationAttempt() {}

    public CommunicationAttempt(UUID id, String requestId, ChannelType channel, String recipient,
                                UUID clientId, String clientName, UUID membershipId,
                                String templateName, String payloadSummary) {
        this.id = id != null ? id : UUID.randomUUID();
        this.requestId = requestId;
        this.channel = channel;
        this.recipient = recipient;
        this.recipientMasked = maskRecipient(recipient);
        this.clientId = clientId;
        this.clientName = clientName;
        this.membershipId = membershipId;
        this.templateName = templateName;
        this.payloadSummary = payloadSummary;
        this.status = CommunicationStatus.SOLICITADO;
        this.attemptCount = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static String maskRecipient(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        String trimmed = raw.trim();
        if (trimmed.contains("@")) {
            int atIdx = trimmed.indexOf("@");
            if (atIdx <= 2) return "***" + trimmed.substring(atIdx);
            return trimmed.substring(0, 2) + "***" + trimmed.substring(atIdx);
        }
        if (trimmed.length() >= 8) {
            return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 2);
        }
        return "****";
    }

    public UUID getId() { return id; }
    public String getRequestId() { return requestId; }
    public ChannelType getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getRecipientMasked() { return recipientMasked; }
    public UUID getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public UUID getMembershipId() { return membershipId; }
    public String getTemplateName() { return templateName; }
    public String getPayloadSummary() { return payloadSummary; }
    public CommunicationStatus getStatus() { return status; }
    public String getExternalMessageId() { return externalMessageId; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getReadAt() { return readAt; }

    public void setStatus(CommunicationStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
        this.updatedAt = LocalDateTime.now();
    }
}

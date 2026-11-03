package com.gymvip.comms.domain;

import java.util.List;

/**
 * Puerto de salida para el despacho de mensajes WhatsApp.
 * HU-17 T03.
 */
public interface WhatsAppSender {

    record WhatsAppSendRequest(
            String phoneNumber,
            String templateName,
            String language,
            List<String> parameters
    ) {}

    record SendResult(
            boolean success,
            String externalId,
            CommunicationStatus status,
            String errorMessage,
            boolean retryable
    ) {
        public static SendResult success(String externalId, CommunicationStatus status) {
            return new SendResult(true, externalId, status, null, false);
        }

        public static SendResult failure(String errorMessage, boolean retryable) {
            return new SendResult(false, null, CommunicationStatus.FALLIDO, errorMessage, retryable);
        }
    }

    SendResult send(WhatsAppSendRequest request);
}

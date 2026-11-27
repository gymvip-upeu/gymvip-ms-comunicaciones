package com.gymvip.comms.domain;

import java.util.List;

/**
 * Puerto de salida para el despacho de mensajes WhatsApp.
 * HU-17 T03.
 * Compatible con formato E.164, BSUID y Usernames (Meta WhatsApp 2026).
 */
public interface WhatsAppSender {

    record WhatsAppSendRequest(
            String phoneNumber,
            String templateName,
            String language,
            List<String> parameters,
            RecipientType recipientType,
            String recipientAddress,
            boolean authenticationCode
    ) {
        public WhatsAppSendRequest(String phoneNumber, String templateName, String language, List<String> parameters,
                                   RecipientType recipientType, String recipientAddress) {
            this(phoneNumber, templateName, language, parameters, recipientType, recipientAddress, false);
        }

        @Override public String toString() {
            return "WhatsAppSendRequest[template=" + templateName + ", contenido omitido]";
        }

        public WhatsAppSendRequest(String phoneNumber, String templateName, String language, List<String> parameters) {
            this(phoneNumber, templateName, language, parameters,
                    RecipientType.inferFromAddress(phoneNumber), phoneNumber);
        }
    }

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

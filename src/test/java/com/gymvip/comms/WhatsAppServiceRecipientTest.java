package com.gymvip.comms;

import com.gymvip.comms.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de validación y soporte de destinatarios WhatsApp (E.164, BSUID y Usernames).
 * Cumplimiento Meta Cloud API v21/v22 y LPDP Ley 29733.
 */
class WhatsAppServiceRecipientTest {

    private WhatsAppSender mockSender;
    private CommunicationAttemptRepository mockRepo;
    private WhatsAppService service;

    @BeforeEach
    void setUp() {
        mockSender = mock(WhatsAppSender.class);
        mockRepo = mock(CommunicationAttemptRepository.class);
        service = new WhatsAppService(mockSender, mockRepo);

        when(mockRepo.findByRequestId(any())).thenReturn(Optional.empty());
        when(mockRepo.save(any(CommunicationAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Inferencia automática de RecipientType según formato de entrada")
    void testRecipientTypeInference() {
        assertEquals(RecipientType.USERNAME, RecipientType.inferFromAddress("@carlos_vip"));
        assertEquals(RecipientType.USERNAME, RecipientType.inferFromAddress("carlos_vip"));
        assertEquals(RecipientType.BSUID, RecipientType.inferFromAddress("bsuid.1234567890abcdef"));
        assertEquals(RecipientType.BSUID, RecipientType.inferFromAddress("bsuid_9876543210"));
        assertEquals(RecipientType.PHONE_NUMBER, RecipientType.inferFromAddress("+51987654321"));
        assertEquals(RecipientType.PHONE_NUMBER, RecipientType.inferFromAddress("987654321"));
    }

    @Test
    @DisplayName("Enmascaramiento seguro de LPDP Ley 29733 para teléfono, BSUID y Username")
    void testMaskRecipientSecurity() {
        // Teléfono
        String maskedPhone = CommunicationAttempt.maskRecipient("+51987654321", RecipientType.PHONE_NUMBER);
        assertTrue(maskedPhone.startsWith("+519"));
        assertTrue(maskedPhone.endsWith("21"));
        assertTrue(maskedPhone.contains("****"));

        // Username
        String maskedUser = CommunicationAttempt.maskRecipient("@carlos_fit", RecipientType.USERNAME);
        assertEquals("@ca***it", maskedUser);

        // BSUID
        String maskedBsuid = CommunicationAttempt.maskRecipient("bsuid.1234567890abcdef", RecipientType.BSUID);
        assertTrue(maskedBsuid.startsWith("bsuid."));
        assertTrue(maskedBsuid.endsWith("cdef"));
        assertTrue(maskedBsuid.contains("***"));

        // Email
        String maskedEmail = CommunicationAttempt.maskRecipient("recepcion@gymvip.pe");
        assertEquals("re***@gymvip.pe", maskedEmail);
    }

    @Test
    @DisplayName("Despacho exitoso con identificador BSUID de Meta")
    void testSendWithBsuid() {
        when(mockSender.send(any())).thenReturn(
                WhatsAppSender.SendResult.success("wamid.bsuid_meta_123", CommunicationStatus.ACEPTADO_PROVEEDOR)
        );

        var request = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-BSUID-001",
                null,
                "recordatorio_vencimiento",
                "es",
                List.of("Valeria Mendoza", "Plan VIP Mensual", "2026-12-01", "5"),
                true,
                UUID.randomUUID(),
                "Valeria Mendoza",
                UUID.randomUUID(),
                RecipientType.BSUID,
                "bsuid.meta_user_9988776655"
        );

        var response = service.processSendRequest(request);

        assertNotNull(response);
        assertEquals(CommunicationStatus.ACEPTADO_PROVEEDOR, response.status());
        assertEquals("wamid.bsuid_meta_123", response.externalMessageId());

        ArgumentCaptor<WhatsAppSender.WhatsAppSendRequest> captor = ArgumentCaptor.forClass(WhatsAppSender.WhatsAppSendRequest.class);
        verify(mockSender).send(captor.capture());
        assertEquals(RecipientType.BSUID, captor.getValue().recipientType());
        assertEquals("bsuid.meta_user_9988776655", captor.getValue().recipientAddress());
    }

    @Test
    @DisplayName("Despacho exitoso con identificador Username de WhatsApp")
    void testSendWithUsername() {
        when(mockSender.send(any())).thenReturn(
                WhatsAppSender.SendResult.success("wamid.user_meta_456", CommunicationStatus.ACEPTADO_PROVEEDOR)
        );

        var request = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-USER-001",
                null,
                "recordatorio_vencimiento",
                "es",
                List.of("Mariana Torres", "Plan VIP Semestral", "2026-11-30", "3"),
                true,
                UUID.randomUUID(),
                "Mariana Torres",
                UUID.randomUUID(),
                RecipientType.USERNAME,
                "@mariana_fitness"
        );

        var response = service.processSendRequest(request);

        assertNotNull(response);
        assertEquals(CommunicationStatus.ACEPTADO_PROVEEDOR, response.status());

        ArgumentCaptor<WhatsAppSender.WhatsAppSendRequest> captor = ArgumentCaptor.forClass(WhatsAppSender.WhatsAppSendRequest.class);
        verify(mockSender).send(captor.capture());
        assertEquals(RecipientType.USERNAME, captor.getValue().recipientType());
        assertEquals("@mariana_fitness", captor.getValue().recipientAddress());
    }

    @Test
    @DisplayName("Rechazo de formato inválido para Username y BSUID")
    void testRejectInvalidUsernameAndBsuid() {
        // Username con caracteres ilegales
        var badUserReq = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-BAD-01", null, "recordatorio_vencimiento", "es",
                List.of("A", "B", "C", "1"), true, null, null, null,
                RecipientType.USERNAME, "@inválido con espacios!"
        );
        assertThrows(IllegalArgumentException.class, () -> service.processSendRequest(badUserReq));

        // BSUID con formato inválido
        var badBsuidReq = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-BAD-02", null, "recordatorio_vencimiento", "es",
                List.of("A", "B", "C", "1"), true, null, null, null,
                RecipientType.BSUID, "123" // demasiado corto
        );
        assertThrows(IllegalArgumentException.class, () -> service.processSendRequest(badBsuidReq));
    }
}

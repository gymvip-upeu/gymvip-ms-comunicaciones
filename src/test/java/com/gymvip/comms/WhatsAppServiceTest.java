package com.gymvip.comms;

import com.gymvip.comms.domain.*;
import com.gymvip.comms.infrastructure.SimulationWhatsAppSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WhatsAppServiceTest {

    @Autowired
    private CommunicationAttemptRepository attemptRepository;

    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        attemptRepository.deleteAll();
        SimulationWhatsAppSender sender = new SimulationWhatsAppSender();
        whatsAppService = new WhatsAppService(sender, attemptRepository);
    }

    @Test
    @DisplayName("Debe despachar recordatorio vía simulador con plantilla aprobada e idempotencia (HU-17 T05 / T07)")
    void testSendReminderSuccess() {
        String reqId = "REQ-REM-" + UUID.randomUUID();
        var req = new WhatsAppService.SendWhatsAppNotificationRequest(
                reqId,
                "987654321",
                WhatsAppTemplate.TEMPLATE_EXPIRY_REMINDER,
                "es",
                List.of("Valeria Mendoza", "Plan VIP Mensual", "2026-11-10", "7"),
                true,
                UUID.randomUUID(),
                "Valeria Mendoza",
                UUID.randomUUID()
        );

        var response = whatsAppService.processSendRequest(req);

        assertNotNull(response);
        assertEquals(reqId, response.requestId());
        assertEquals("WHATSAPP", response.channel());
        assertEquals(CommunicationStatus.ENVIADO, response.status());
        assertNotNull(response.externalMessageId());
        assertTrue(response.externalMessageId().startsWith("wamid.sim_"));
        assertTrue(response.recipientMasked().contains("****"));

        // Idempotencia: segundo llamado con el mismo requestId no duplica
        var second = whatsAppService.processSendRequest(req);
        assertEquals(response.id(), second.id());
        assertEquals(1, attemptRepository.count());
    }

    @Test
    @DisplayName("Debe rechazar el envío si el miembro no ha otorgado consentimiento de WhatsApp (HU-17 T06)")
    void testRejectsWithoutConsent() {
        var req = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-NO-CONSENT-" + UUID.randomUUID(),
                "987654321",
                WhatsAppTemplate.TEMPLATE_EXPIRY_REMINDER,
                "es",
                List.of("Carlos Lopez", "Plan Mensual", "2026-11-10", "7"),
                false, // Sin consentimiento
                UUID.randomUUID(),
                "Carlos Lopez",
                UUID.randomUUID()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> whatsAppService.processSendRequest(req));
        assertTrue(ex.getMessage().contains("consentimiento"));
        assertEquals(0, attemptRepository.count());
    }

    @Test
    @DisplayName("Debe rechazar plantillas no aprobadas o arbitrarias (HU-17 T04)")
    void testRejectsUnapprovedTemplate() {
        var req = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-INVALID-TPL-" + UUID.randomUUID(),
                "987654321",
                "promocion_no_autorizada_black_friday",
                "es",
                List.of("Carlos"),
                true,
                UUID.randomUUID(),
                "Carlos",
                UUID.randomUUID()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> whatsAppService.processSendRequest(req));
        assertTrue(ex.getMessage().contains("no está aprobada"));
    }

    @Test
    @DisplayName("Debe rechazar teléfonos inválidos y normalizar celulares peruanos (HU-17 T06)")
    void testPhoneNormalizationAndValidation() {
        assertEquals("+51987654321", whatsAppService.normalizePhoneNumber("987654321"));
        assertEquals("+51987654321", whatsAppService.normalizePhoneNumber("+51 987-654-321"));
        assertNull(whatsAppService.normalizePhoneNumber(""));

        var reqInvalidPhone = new WhatsAppService.SendWhatsAppNotificationRequest(
                "REQ-BAD-PHONE-" + UUID.randomUUID(),
                "123", // Muy corto
                WhatsAppTemplate.TEMPLATE_EXPIRY_REMINDER,
                "es",
                List.of("Carlos", "Plan", "2026-11-10", "3"),
                true,
                UUID.randomUUID(),
                "Carlos",
                UUID.randomUUID()
        );

        assertThrows(IllegalArgumentException.class, () -> whatsAppService.processSendRequest(reqInvalidPhone));
    }
}

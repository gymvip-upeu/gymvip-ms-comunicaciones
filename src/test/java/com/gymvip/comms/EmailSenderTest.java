package com.gymvip.comms;

import com.gymvip.comms.domain.EmailSender;
import com.gymvip.comms.infrastructure.EmailConfig;
import com.gymvip.comms.infrastructure.ResendEmailSender;
import com.gymvip.comms.infrastructure.SimulationEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailSenderTest {

    @Test
    @DisplayName("SimulationEmailSender debe retornar un deliveryId simulado sin fallar")
    void testSimulationEmailSender() {
        SimulationEmailSender sender = new SimulationEmailSender();
        String deliveryId = sender.sendEmail("socio@gymvip.pe", "Bienvenido", "welcome", Map.of("nombre", "Carlos"));
        assertNotNull(deliveryId);
        assertTrue(deliveryId.startsWith("sim_mail_"));
    }

    @Test
    @DisplayName("ResendEmailSender sin API key configurada debe fallar con IllegalStateException")
    void testResendEmailSenderUnconfigured() {
        ResendEmailSender sender = new ResendEmailSender("", "notificaciones@gymvip.pe");
        assertFalse(sender.isConfigured());
        assertThrows(IllegalStateException.class, () ->
                sender.sendEmail("socio@gymvip.pe", "Test", "welcome", Map.of())
        );
    }

    @Test
    @DisplayName("EmailConfig debe seleccionar SimulationEmailSender en modo simulacion")
    void testEmailConfigSimulationFallback() {
        EmailConfig config = new EmailConfig();
        SimulationEmailSender simSender = new SimulationEmailSender();
        ResendEmailSender resendSender = new ResendEmailSender("", "notificaciones@gymvip.pe");

        EmailSender selected = config.primaryEmailSender("simulation", "resend", resendSender, simSender);
        assertTrue(selected instanceof SimulationEmailSender);
    }

    @Test
    @DisplayName("EmailConfig debe seleccionar ResendEmailSender en producción con credencial válida")
    void testEmailConfigProductionResend() {
        EmailConfig config = new EmailConfig();
        SimulationEmailSender simSender = new SimulationEmailSender();
        ResendEmailSender resendSender = new ResendEmailSender("re_test_dummy_key_12345", "notificaciones@gymvip.pe");

        EmailSender selected = config.primaryEmailSender("production", "resend", resendSender, simSender);
        assertTrue(selected instanceof ResendEmailSender);
    }
}

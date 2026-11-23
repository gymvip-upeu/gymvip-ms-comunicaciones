package com.gymvip.comms;

import com.gymvip.comms.api.WhatsAppChannelStatusController;
import com.gymvip.comms.infrastructure.MetaWhatsAppSender;
import com.gymvip.comms.infrastructure.SimulationWhatsAppSender;
import com.gymvip.comms.infrastructure.WhatsAppConfig;
import com.gymvip.comms.infrastructure.security.ServiceKeyAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WhatsAppChannelStatusTest {
    private final SimulationWhatsAppSender simulation = new SimulationWhatsAppSender();
    private MetaWhatsAppSender meta(String id,String token) { return new MetaWhatsAppSender(id,token,"v20.0",simulation); }
    @SuppressWarnings("unchecked")
    private Map<String,Object> report(boolean enabled,String mode,String provider,MetaWhatsAppSender sender) {
        return (Map<String,Object>)((Map<?,?>)new WhatsAppChannelStatusController(enabled,mode,provider,sender).status().get("channels")).get("whatsapp");
    }
    @Test void explicitSimulationRemainsSimulationEvenWithCredentials() {
        assertEquals("SIMULATION",report(true,"simulation","meta",meta("123456","valid-secret")).get("status"));
        assertEquals("SIMULATION",report(true,"production","simulation",meta("123456","valid-secret")).get("status"));
    }
    @Test void missingOrPlaceholderCredentialsCannotCertifyRealConfiguration() {
        for(var sender: new MetaWhatsAppSender[]{meta("",""),meta("123456","placeholder"),meta("replace-phone","valid-secret")}) {
            var report=report(true,"production","meta",sender);
            assertEquals("UNCONFIGURED",report.get("status"));
            assertEquals("BLOCKED",report.get("effectiveMode"));
        }
    }
    @Test void completeConfigurationDoesNotCertifyProviderConnectionOrDelivery() {
        var report=report(true,"production","meta",meta("123456","valid-secret"));
        assertEquals("CONFIGURED_REAL",report.get("status"));
        assertEquals(false,report.get("connectionVerified"));
        assertFalse(report.toString().contains("valid-secret"));
        assertFalse(report.toString().contains("123456"));
    }
    @Test void disabledChannelActuallyRejectsDispatchWithoutContactingProvider() {
        var real=meta("123456","valid-secret");
        assertEquals("DISABLED",report(false,"production","meta",real).get("status"));
        var sender=new WhatsAppConfig().primaryWhatsAppSender("production","meta",false,real,simulation);
        assertFalse(sender.send(null).success());
    }
    @Test void reportRequiresInternalServiceKeyAndExposesNoEmailOrPushClaims() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new WhatsAppChannelStatusController(true,"simulation","meta",meta("","")))
            .addFilters(new ServiceKeyAuthFilter("shared-private-test-key","simulation")).build();
        mvc.perform(get("/api/v1/channels/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/channels/status").header("X-Service-Key","shared-private-test-key"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.channels.whatsapp.status").value("SIMULATION"))
            .andExpect(jsonPath("$.channels.email").doesNotExist()).andExpect(jsonPath("$.channels.push").doesNotExist());
    }

    @Test void productionWithoutCredentialsFailsInsteadOfSimulating() {
        var sender=new WhatsAppConfig().primaryWhatsAppSender("production","meta",true,meta("",""),simulation);
        assertFalse(sender.send(null).success());
        assertSame(simulation,new WhatsAppConfig().primaryWhatsAppSender("simulation","meta",true,meta("123456","valid-secret"),simulation));
    }
}

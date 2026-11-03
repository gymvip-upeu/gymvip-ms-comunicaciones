package com.gymvip.comms;

import com.gymvip.comms.domain.ChannelType;
import com.gymvip.comms.domain.CommunicationAttempt;
import com.gymvip.comms.domain.CommunicationAttemptRepository;
import com.gymvip.comms.domain.CommunicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommunicationsQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunicationAttemptRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/communications debe listar intentos con filtros y paginación (HU-18 T06 / T10)")
    void testListCommunicationsWithFilters() throws Exception {
        CommunicationAttempt c1 = new CommunicationAttempt(
                UUID.randomUUID(), "REQ-01", ChannelType.WHATSAPP, "+51987654321",
                UUID.randomUUID(), "Juan Perez", UUID.randomUUID(), "recordatorio_vencimiento", "summary"
        );
        c1.setStatus(CommunicationStatus.ENTREGADO);
        repository.save(c1);

        CommunicationAttempt c2 = new CommunicationAttempt(
                UUID.randomUUID(), "REQ-02", ChannelType.WHATSAPP, "+51912345678",
                UUID.randomUUID(), "Maria Rojas", UUID.randomUUID(), "aviso_vencido", "summary"
        );
        c2.setStatus(CommunicationStatus.FALLIDO);
        c2.setErrorMessage("Error de red simulado");
        repository.save(c2);

        // Sin filtros: retorna 2 elementos
        mockMvc.perform(get("/api/v1/communications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Filtro por status=FALLIDO: retorna 1 elemento
        mockMvc.perform(get("/api/v1/communications").param("status", "FALLIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].clientName", is("Maria Rojas")));

        // Filtro por búsqueda search=Juan
        mockMvc.perform(get("/api/v1/communications").param("search", "Juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].clientName", is("Juan Perez")));
    }
}

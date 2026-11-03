package com.gymvip.comms.api;

import com.gymvip.comms.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoints de consulta y gestión de estados de comunicación.
 * HU-18 T06, T07, T08.
 */
@RestController
@RequestMapping("/api/v1/communications")
public class CommunicationsQueryController {

    private final CommunicationAttemptRepository repository;
    private final WhatsAppService whatsAppService;

    public CommunicationsQueryController(
            CommunicationAttemptRepository repository,
            WhatsAppService whatsAppService
    ) {
        this.repository = repository;
        this.whatsAppService = whatsAppService;
    }

    public record CommunicationItemDto(
            UUID id,
            String requestId,
            String channel,
            String recipientMasked,
            UUID clientId,
            String clientName,
            UUID membershipId,
            String templateName,
            String payloadSummary,
            String status,
            String externalMessageId,
            String errorMessage,
            int attemptCount,
            String createdAt,
            String sentAt,
            String deliveredAt,
            String readAt
    ) {
        public static CommunicationItemDto from(CommunicationAttempt a) {
            return new CommunicationItemDto(
                    a.getId(),
                    a.getRequestId(),
                    a.getChannel().name(),
                    a.getRecipientMasked(),
                    a.getClientId(),
                    a.getClientName() != null ? a.getClientName() : "Socio GYM VIP",
                    a.getMembershipId(),
                    a.getTemplateName(),
                    a.getPayloadSummary(),
                    a.getStatus().name(),
                    a.getExternalMessageId(),
                    a.getErrorMessage(),
                    a.getAttemptCount(),
                    a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                    a.getSentAt() != null ? a.getSentAt().toString() : null,
                    a.getDeliveredAt() != null ? a.getDeliveredAt().toString() : null,
                    a.getReadAt() != null ? a.getReadAt().toString() : null
            );
        }
    }

    @GetMapping
    public ResponseEntity<?> listCommunications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CommunicationStatus st = null;
        if (status != null && !status.isBlank() && !"TODOS".equalsIgnoreCase(status)) {
            try { st = CommunicationStatus.valueOf(status.trim().toUpperCase()); } catch (Exception ignored) {}
        }

        ChannelType ch = null;
        if (channel != null && !channel.isBlank() && !"TODOS".equalsIgnoreCase(channel)) {
            try { ch = ChannelType.valueOf(channel.trim().toUpperCase()); } catch (Exception ignored) {}
        }

        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));

        Page<CommunicationAttempt> paged = repository.findWithFilters(st, ch, term, pageRequest);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", paged.getContent().stream().map(CommunicationItemDto::from).toList());
        response.put("totalElements", paged.getTotalElements());
        response.put("totalPages", paged.getTotalPages());
        response.put("currentPage", paged.getNumber());
        response.put("size", paged.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCommunicationById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(CommunicationItemDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryCommunication(@PathVariable UUID id) {
        try {
            WhatsAppService.CommunicationResponse res = whatsAppService.retryAttempt(id);
            return ResponseEntity.ok(res);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

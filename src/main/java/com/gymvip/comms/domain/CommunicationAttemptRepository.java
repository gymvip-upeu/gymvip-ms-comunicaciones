package com.gymvip.comms.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunicationAttemptRepository extends JpaRepository<CommunicationAttempt, UUID>, JpaSpecificationExecutor<CommunicationAttempt> {

    Optional<CommunicationAttempt> findByRequestId(String requestId);

    Optional<CommunicationAttempt> findByExternalMessageId(String externalMessageId);

    default Page<CommunicationAttempt> findWithFilters(
            CommunicationStatus status,
            ChannelType channel,
            String search,
            Pageable pageable
    ) {
        Specification<CommunicationAttempt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (search != null && !search.isBlank()) {
                String escaped = search.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                Predicate clientNamePredicate = cb.like(cb.lower(root.get("clientName")), pattern, '\\');
                Predicate recipientMaskedPredicate = cb.like(cb.lower(root.get("recipientMasked")), pattern, '\\');
                Predicate recipientPredicate = cb.like(cb.lower(root.get("recipient")), pattern, '\\');
                predicates.add(cb.or(clientNamePredicate, recipientMaskedPredicate, recipientPredicate));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}

package com.gymvip.comms.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunicationAttemptRepository extends JpaRepository<CommunicationAttempt, UUID> {

    Optional<CommunicationAttempt> findByRequestId(String requestId);

    Optional<CommunicationAttempt> findByExternalMessageId(String externalMessageId);

    @Query("""
        SELECT c FROM CommunicationAttempt c
        WHERE (:status IS NULL OR c.status = :status)
          AND (:channel IS NULL OR c.channel = :channel)
          AND (:search IS NULL OR LOWER(c.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
                             OR LOWER(c.recipient) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.createdAt DESC
    """)
    Page<CommunicationAttempt> findWithFilters(
            @Param("status") CommunicationStatus status,
            @Param("channel") ChannelType channel,
            @Param("search") String search,
            Pageable pageable
    );
}

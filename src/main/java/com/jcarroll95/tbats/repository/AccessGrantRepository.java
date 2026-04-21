package com.jcarroll95.tbats.repository;
import com.jcarroll95.tbats.model.AccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;


public interface AccessGrantRepository extends JpaRepository<AccessGrant, UUID>{

    List<AccessGrant> findByUserId(UUID userId);

    List<AccessGrant> findByUserIdAndRevokedFalse(UUID userId);

    List<AccessGrant> findByExpiresAtBeforeAndRevokedFalse(OffsetDateTime now);

    List<AccessGrant> findByUserIdAndRevokedFalseAndExpiresAtAfter(UUID userId, OffsetDateTime now);

    List<AccessGrant> findByRevokedFalseAndExpiresAtAfter(OffsetDateTime now);

}

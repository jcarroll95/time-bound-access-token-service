package com.jcarroll95.tbats.scheduler;

import com.jcarroll95.tbats.model.AccessGrant;
import com.jcarroll95.tbats.repository.AccessGrantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class GrantExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(GrantExpirationJob.class);

    private final AccessGrantRepository grantRepository;

    public GrantExpirationJob(AccessGrantRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    @Scheduled(fixedDelayString = "${grants.expiration.interval-ms:60000}")
    public void revokeExpiredGrants() {
        OffsetDateTime now = OffsetDateTime.now();
        List<AccessGrant> expired = grantRepository
                .findByExpiresAtBeforeAndRevokedFalse(now);

        if (expired.isEmpty()) {
            return;
        }

        expired.forEach(g -> g.setRevoked(true));
        grantRepository.saveAll(expired);

        log.info("Scheduled revocation: flipped {} expired grants", expired.size());
    }
}
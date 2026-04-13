package com.jcarroll95.tbats;

import com.jcarroll95.tbats.model.AccessGrant;
import com.jcarroll95.tbats.model.Role;
import com.jcarroll95.tbats.model.User;
import com.jcarroll95.tbats.repository.AccessGrantRepository;
import com.jcarroll95.tbats.repository.UserRepository;
import com.jcarroll95.tbats.scheduler.GrantExpirationJob;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("See docs/integration-tests.md — runtime ecosystem incompatibility")
class GrantExpirationIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired AccessGrantRepository grantRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired GrantExpirationJob job;
    @Autowired EntityManager em;

    @Test
    @Transactional
    void expirationJobRevokesPastDueGrants() {
        userRepository.deleteAll();
        grantRepository.deleteAll();

        User alice = new User("alice", passwordEncoder.encode("pw"), Role.USER);
        userRepository.save(alice);

        AccessGrant grant = new AccessGrant(alice.getId(), "resource", 60);
        grantRepository.save(grant);
        UUID grantId = grant.getId();

        // Force expiration into the past via direct SQL — the domain API
        // doesn't expose expiresAt mutation (by design), but tests need to
        // simulate time without actually waiting.
        em.createNativeQuery("UPDATE access_grants SET expires_at = :past WHERE id = :id")
                .setParameter("past", OffsetDateTime.now().minusMinutes(5))
                .setParameter("id", grantId)
                .executeUpdate();
        em.clear();  // drop first-level cache so the next read sees fresh DB state

        job.revokeExpiredGrants();

        AccessGrant refreshed = grantRepository.findById(grantId).orElseThrow();
        assertThat(refreshed.getRevoked()).isTrue();
    }
}
package com.jcarroll95.tbats.service;

import com.jcarroll95.tbats.dto.grant.AdminGrantResponse;
import com.jcarroll95.tbats.model.User;
import com.jcarroll95.tbats.repository.AccessGrantRepository;
import com.jcarroll95.tbats.repository.UserRepository;
import com.jcarroll95.tbats.dto.grant.GrantResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.jcarroll95.tbats.model.AccessGrant;
import com.jcarroll95.tbats.dto.grant.CreateGrantRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.jcarroll95.tbats.model.Role;
@Service
public class GrantService {

    private final AccessGrantRepository grantRepository;
    private final UserRepository userRepository;

    // Constructor injection add in
    public GrantService(AccessGrantRepository grantRepository, UserRepository userRepository) {
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
    }

    public GrantResponse createGrant(String callerUsername, CreateGrantRequest request) {
        User caller = userRepository.findByUsername(callerUsername)
                .orElseThrow(() -> new EntityNotFoundException(callerUsername));

        if (caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Admin role required to create grants");
        }

        User target = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new EntityNotFoundException(request.username()));

        int duration = request.durationMinutes();
        if (duration < 1 || duration > 360) throw new IllegalArgumentException("Request duration unacceptable");

        AccessGrant grant = new AccessGrant(target.getId(), request.resourceName(), duration);
        AccessGrant savedGrant = grantRepository.save(grant);
        return toResponse(savedGrant);
    }

    public GrantResponse getGrant(UUID grantId, String username) {
        AccessGrant grant = grantRepository.findById(grantId).orElseThrow(() -> new EntityNotFoundException(grantId.toString()));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException(username));
        assertCanAccess(grant, user);
        return toResponse(grant);
    }

    public List<GrantResponse> getActiveGrantsForUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException(username));
        List<AccessGrant> activeGrants = grantRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(user.getId(), OffsetDateTime.now());
        return activeGrants.stream().map(this::toResponse).toList();
    }

    public void revokeGrant(UUID grantId, String username) {
        AccessGrant grant = grantRepository.findById(grantId).orElseThrow(() -> new EntityNotFoundException(grantId.toString()));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException(username));
        assertCanAccess(grant, user);
        grant.setRevoked(true);
        grantRepository.save(grant);
    }

    public List<AdminGrantResponse> getAllActiveGrants(String username) {
        List<AccessGrant> grants = grantRepository.findByRevokedFalseAndExpiresAtAfter(OffsetDateTime.now());

        Map<UUID, String> userLookup = userRepository.findAllById(
                grants.stream().map(AccessGrant::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));

        return grants.stream().map(g -> toAdminResponse(g, userLookup)).toList();
    }

    private GrantResponse toResponse(AccessGrant grant) {
        boolean active = !grant.getRevoked() && grant.getExpiresAt().isAfter(OffsetDateTime.now());
        return new GrantResponse(
                grant.getId(),
                grant.getUserId(),
                grant.getResourceName(),
                grant.getIssuedAt(),
                grant.getExpiresAt(),
                grant.getRevoked(),
                active
        );
    }

    private void assertCanAccess(AccessGrant grant, User user) {
        boolean isOwner = grant.getUserId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("User does not have access to this grant");
        }
    }

    private AdminGrantResponse toAdminResponse(AccessGrant grant, Map<UUID, String> userLookup) {
        return new AdminGrantResponse(
                grant.getId(),
                grant.getUserId(),
                userLookup.getOrDefault(grant.getUserId(), "unknown"),
                grant.getResourceName(),
                grant.getIssuedAt(),
                grant.getExpiresAt(),
                grant.getRevoked(),
                !grant.getRevoked() && grant.getExpiresAt().isAfter(OffsetDateTime.now())
        );
    }

}
package com.jcarroll95.tbats.controller;

import com.jcarroll95.tbats.dto.grant.CreateGrantRequest;
import com.jcarroll95.tbats.dto.grant.GrantListResponse;
import com.jcarroll95.tbats.dto.grant.GrantResponse;
import com.jcarroll95.tbats.service.GrantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/grants")
public class GrantController {

    private final GrantService grantService;

    public GrantController(GrantService grantService) {
        this.grantService = grantService;
    }

    @PostMapping
    public ResponseEntity<GrantResponse> create(
            @Valid @RequestBody CreateGrantRequest request,
            Authentication auth) {
        GrantResponse response = grantService.createGrant(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrantResponse> getOne(
            @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(
                grantService.getGrant(id, auth.getName())
        );
    }

    @GetMapping("/active")
    public ResponseEntity<GrantListResponse> getActive(Authentication auth) {
        List<GrantResponse> grants = grantService.getActiveGrantsForUser(auth.getName());
        return ResponseEntity.ok(new GrantListResponse(grants));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID id,
            Authentication auth) {
        grantService.revokeGrant(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

}
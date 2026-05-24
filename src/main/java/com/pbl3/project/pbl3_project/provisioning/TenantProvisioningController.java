package com.pbl3.project.pbl3_project.provisioning;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/provisioning")
@Profile("provisioning")
public class TenantProvisioningController {

    private final TenantProvisioningService provisioningService;
    private final String apiKey;

    public TenantProvisioningController(
        TenantProvisioningService provisioningService,
        @Value("${provisioning.api-key:}") String apiKey
    ) {
        this.provisioningService = provisioningService;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/businesses")
    public ResponseEntity<TenantProvisioningService.TenantProvisioningResponse> createBusiness(
        @RequestHeader(value = "X-Provisioning-Key", required = false) String providedApiKey,
        @RequestBody TenantProvisioningService.CreateBusinessRequest request
    ) {
        requireApiKey(providedApiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(provisioningService.createBusiness(request));
    }

    @GetMapping("/businesses/{businessCode}")
    public TenantProvisioningService.TenantPreviewResponse previewBusinessLegacy(
        @RequestHeader(value = "X-Provisioning-Key", required = false) String providedApiKey,
        @PathVariable String businessCode
    ) {
        requireApiKey(providedApiKey);
        return provisioningService.previewBusiness(businessCode);
    }

    @GetMapping("/businesses/{businessCode}/preview")
    public TenantProvisioningService.TenantPreviewResponse previewBusiness(
        @RequestHeader(value = "X-Provisioning-Key", required = false) String providedApiKey,
        @PathVariable String businessCode
    ) {
        requireApiKey(providedApiKey);
        return provisioningService.previewBusiness(businessCode);
    }

    @PostMapping("/businesses/{businessCode}/connect")
    public TenantProvisioningService.TenantConnectionResponse connectBusiness(
        @RequestHeader(value = "X-Provisioning-Key", required = false) String providedApiKey,
        @PathVariable String businessCode,
        @RequestBody TenantProvisioningService.JoinBusinessRequest request
    ) {
        requireApiKey(providedApiKey);
        return provisioningService.connectBusiness(businessCode, request);
    }

    @PostMapping("/businesses/{businessCode}/join-pin/reset")
    public TenantProvisioningService.TenantJoinPinResetResponse resetJoinPin(
        @RequestHeader(value = "X-Provisioning-Key", required = false) String providedApiKey,
        @PathVariable String businessCode
    ) {
        requireApiKey(providedApiKey);
        return provisioningService.resetJoinPin(businessCode);
    }

    private void requireApiKey(String providedApiKey) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Provisioning API key is not configured");
        }
        if (!apiKey.equals(providedApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid provisioning key");
        }
    }
}

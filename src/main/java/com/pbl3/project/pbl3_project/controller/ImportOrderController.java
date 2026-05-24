package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ApiSessionService;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import-orders")
public class ImportOrderController {
    private final ApiSessionService apiSessionService;
    private final AuthorizationService authorizationService;

    public ImportOrderController(ApiSessionService apiSessionService, AuthorizationService authorizationService) {
        this.apiSessionService = apiSessionService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<String> createImportOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        authorizationService.requireImportGoodsAccess(actor);
        return ResponseEntity.status(501).body("Import Orders API is not implemented yet");
    }

    @GetMapping
    public ResponseEntity<String> getAllImportOrders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        authorizationService.requireImportGoodsAccess(actor);
        return ResponseEntity.status(501).body("Import Orders API is not implemented yet");
    }
}

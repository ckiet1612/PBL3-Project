package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalAuditLog;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.OperationalAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalAuditLogServiceTest {

    @Mock
    private OperationalAuditLogRepository repository;
    @Mock
    private AuthorizationService authorizationService;

    private OperationalAuditLogService service;

    @BeforeEach
    void setUp() {
        service = new OperationalAuditLogService(repository, authorizationService);
    }

    @Test
    void recordPersistsBusinessAuditPayload() {
        User actor = new User();
        actor.setId(5L);
        actor.setUsername("manager");

        when(repository.save(any(OperationalAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OperationalAuditLog saved = service.record(
            actor,
            OperationalAuditAction.ORDER_CREATED,
            OperationalSubjectType.ORDER,
            77L,
            "Order #77",
            "Order created"
        );

        assertEquals(actor, saved.getActor());
        assertEquals(OperationalAuditAction.ORDER_CREATED, saved.getAction());
        assertEquals(OperationalSubjectType.ORDER, saved.getSubjectType());
        assertEquals(77L, saved.getSubjectId());
        assertEquals("Order #77", saved.getSubjectLabel());
        assertEquals("Order created", saved.getDetails());
    }

    @Test
    void searchRequiresAuditAccessAndDelegatesToRepository() {
        User viewer = new User();
        viewer.setId(9L);
        viewer.setUsername("admin");

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.searchOperationalAuditLogs(
            viewer,
            "order",
            null,
            null,
            Set.of("admin"),
            Set.of(OperationalAuditAction.ORDER_CREATED),
            Set.of(OperationalSubjectType.ORDER),
            PageRequest.of(0, 10)
        );

        verify(authorizationService).requireAuditLogAccess(eq(viewer));
        verify(repository).findAll(any(Specification.class), any(PageRequest.class));
    }
}

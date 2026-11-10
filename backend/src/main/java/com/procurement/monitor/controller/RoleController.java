package com.procurement.monitor.controller;

import com.procurement.monitor.dto.response.RoleResponse;
import com.procurement.monitor.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final UserService service;

    public RoleController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<RoleResponse> list() {
        return service.availableRoles();
    }
}

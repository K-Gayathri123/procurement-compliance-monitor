package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.*;
import com.procurement.monitor.exception.ApiException;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.validation.Checks;
import com.procurement.monitor.validation.PasswordRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log =
        LoggerFactory.getLogger(UserService.class);

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder passwords;

    public UserService(
        UserAccountRepository users,
        RoleRepository roles,
        UserRoleRepository userRoles,
        PasswordEncoder passwords
    ) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAccountResponse> list(
        int page,
        int size,
        String sort,
        String query,
        boolean includeDeleted
    ) {
        Checks.require(
            Set.of("userId", "fullName", "email", "active")
                .contains(sort),
            "Unsupported user sort field"
        );

        Specification<UserAccount> specification =
            (root, criteria, builder) -> {

                var predicates = new ArrayList<
                    jakarta.persistence.criteria.Predicate>();

                if (!includeDeleted) {
                    predicates.add(
                        builder.isNull(root.get("deletedAt"))
                    );
                }

                if (query != null && !query.isBlank()) {
                    String pattern = "%"
                        + query.trim().toLowerCase(Locale.ROOT)
                        + "%";

                    predicates.add(
                        builder.or(
                            builder.like(
                                builder.lower(root.get("fullName")),
                                pattern
                            ),
                            builder.like(
                                builder.lower(root.get("email")),
                                pattern
                            )
                        )
                    );
                }

                return builder.and(
                    predicates.toArray(
                        jakarta.persistence.criteria.Predicate[]::new
                    )
                );
            };

        var result = users.findAll(
            specification,
            PageRequest.of(
                page,
                size,
                Sort.by(sort).ascending()
            )
        ).map(ResponseMapper::toResponse);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse get(Long id) {
        return ResponseMapper.toResponse(
            Checks.found(users.findById(id), "User not found")
        );
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> availableRoles() {
        return roles.findAll(
            Sort.by("roleId").ascending()
        ).stream()
            .map(ResponseMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> assignedRoles(Long userId) {
        Checks.found(users.findById(userId), "User not found");

        return userRoles.findByUserId(userId).stream()
            .map(link -> roles.findById(link.getRoleId()))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(Role::getRoleId))
            .map(ResponseMapper::toResponse)
            .toList();
    }

    @Transactional
    public UserAccountResponse create(
        CreateUserRequest request,
        Long actorId
    ) {
        PasswordRules.validateNewPassword(request.password());

        String email = normalizeEmail(request.email());
        validateUniqueEmail(email, null);
        validateRoleIds(request.roleIds());

        var user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
            passwords.encode(request.password())
        );
        user.setActive(true);

        user = users.saveAndFlush(user);

        synchronizeRoles(
            user.getUserId(), request.roleIds()
        );

        log.info(
            "User created; userId={}, actorId={}",
            user.getUserId(), actorId
        );

        return ResponseMapper.toResponse(user);
    }

    @Transactional
    public UserAccountResponse update(
        Long id,
        UpdateUserRequest request,
        Long actorId
    ) {
        var user = Checks.found(
            users.findById(id), "User not found"
        );

        Checks.conflict(
            user.getDeletedAt() != null,
            "Restore the user before updating"
        );

        String email = normalizeEmail(request.email());
        validateUniqueEmail(email, id);

        if (!request.active()) {
            Checks.conflict(
                id.equals(actorId),
                "You cannot deactivate your own account"
            );
            protectLastActiveAdmin(id);
        } else {
            Checks.require(
                user.getPasswordHash() != null,
                "Configure a password before activation"
            );
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setActive(request.active());

        log.info(
            "User updated; userId={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(
            users.save(user)
        );
    }

    @Transactional
    public List<RoleResponse> assignRoles(
        Long id,
        RoleAssignmentRequest request,
        Long actorId
    ) {
        var user = Checks.found(
            users.findById(id), "User not found"
        );

        Checks.conflict(
            user.getDeletedAt() != null,
            "Restore the user before assigning roles"
        );

        validateRoleIds(request.roleIds());

        boolean keepsAdmin = request.roleIds().stream()
            .map(roleId -> Checks.found(
                roles.findById(roleId), "Role not found"
            ))
            .anyMatch(role ->
                "ADMIN".equals(role.getRoleName())
            );

        if (!keepsAdmin) {
            protectLastActiveAdmin(id);
        }

        synchronizeRoles(id, request.roleIds());

        log.info(
            "User roles changed; userId={}, actorId={}",
            id, actorId
        );

        return assignedRoles(id);
    }

    @Transactional
    public void resetPassword(
        Long id,
        PasswordResetRequest request,
        Long actorId
    ) {
        var user = Checks.found(
            users.findById(id), "User not found"
        );

        Checks.conflict(
            user.getDeletedAt() != null,
            "Restore the user before resetting its password"
        );

        PasswordRules.validateNewPassword(
            request.newPassword()
        );

        user.setPasswordHash(
            passwords.encode(request.newPassword())
        );

        users.save(user);

        log.info(
            "Password reset; userId={}, actorId={}",
            id, actorId
        );
    }

    @Transactional
    public void changeOwnPassword(
        Long userId,
        ChangePasswordRequest request
    ) {
        var user = Checks.found(
            users.findById(userId), "User not found"
        );

        Checks.require(
            Boolean.TRUE.equals(user.getActive())
                && user.getDeletedAt() == null,
            "Account is unavailable"
        );

        boolean matches =
            user.getPasswordHash() != null
                && PasswordRules.supportedLoginInput(
                    request.currentPassword()
                )
                && passwords.matches(
                    request.currentPassword(),
                    user.getPasswordHash()
                );

        if (!matches) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Current password is incorrect"
            );
        }

        PasswordRules.validateNewPassword(
            request.newPassword()
        );

        Checks.require(
            !passwords.matches(
                request.newPassword(),
                user.getPasswordHash()
            ),
            "New password must differ from the current password"
        );

        user.setPasswordHash(
            passwords.encode(request.newPassword())
        );

        users.save(user);

        log.info(
            "Own password changed; userId={}",
            userId
        );
    }

    @Transactional
    public void softDelete(
        Long id,
        ActionRequest request,
        Long actorId
    ) {
        Checks.conflict(
            id.equals(actorId),
            "You cannot delete your own account"
        );

        var user = Checks.found(
            users.findById(id), "User not found"
        );

        Checks.conflict(
            user.getDeletedAt() != null,
            "User is already soft-deleted"
        );

        protectLastActiveAdmin(id);

        user.setActive(false);
        user.setDeletedAt(Instant.now().toString());
        user.setDeletedByUserId(actorId);
        user.setDeletionReason(request.reason().trim());

        users.save(user);

        log.info(
            "User soft-deleted; userId={}, actorId={}",
            id, actorId
        );
    }

    @Transactional
    public UserAccountResponse restore(
        Long id,
        Long actorId
    ) {
        var user = Checks.found(
            users.findById(id), "User not found"
        );

        Checks.conflict(
            user.getDeletedAt() == null,
            "User is not soft-deleted"
        );

        user.setDeletedAt(null);
        user.setDeletedByUserId(null);
        user.setDeletionReason(null);

        // Restoration alone must not grant login access.
        user.setActive(false);

        log.info(
            "User restored inactive; userId={}, actorId={}",
            id, actorId
        );

        return ResponseMapper.toResponse(
            users.save(user)
        );
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUniqueEmail(
        String email,
        Long currentUserId
    ) {
        var existing = users.findByEmail(email);

        Checks.conflict(
            existing.isPresent()
                && !Objects.equals(
                    existing.get().getUserId(),
                    currentUserId
                ),
            "Email is already reserved"
        );
    }

    private void validateRoleIds(Set<Long> roleIds) {
        Checks.require(
            roleIds != null && !roleIds.isEmpty(),
            "At least one role is required"
        );

        for (Long roleId : roleIds) {
            Checks.found(
                roles.findById(roleId),
                "Role not found: " + roleId
            );
        }
    }

    private void synchronizeRoles(
        Long userId,
        Set<Long> requestedRoleIds
    ) {
        var existing = userRoles.findByUserId(userId);

        Set<Long> existingIds = existing.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toSet());

        for (UserRole link : existing) {
            if (!requestedRoleIds.contains(link.getRoleId())) {
                userRoles.delete(link);
            }
        }

        for (Long roleId : requestedRoleIds) {
            if (!existingIds.contains(roleId)) {
                var link = new UserRole();
                link.setUserId(userId);
                link.setRoleId(roleId);
                userRoles.save(link);
            }
        }

        userRoles.flush();
    }

    private void protectLastActiveAdmin(Long userId) {
        var adminRole = Checks.found(
            roles.findByRoleName("ADMIN"),
            "ADMIN role is missing"
        );

        Set<Long> adminIds = userRoles
            .findByRoleId(adminRole.getRoleId())
            .stream()
            .map(UserRole::getUserId)
            .collect(Collectors.toSet());

        if (!adminIds.contains(userId)) {
            return;
        }

        var current = Checks.found(
            users.findById(userId), "User not found"
        );

        if (!Boolean.TRUE.equals(current.getActive())
            || current.getDeletedAt() != null) {
            return;
        }

        long activeAdminCount = users.findAllById(adminIds)
            .stream()
            .filter(user ->
                Boolean.TRUE.equals(user.getActive()))
            .filter(user ->
                user.getDeletedAt() == null)
            .count();

        Checks.conflict(
            activeAdminCount <= 1,
            "Cannot remove or deactivate the last active admin"
        );
    }
}

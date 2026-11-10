package com.procurement.monitor.security;

import com.procurement.monitor.dto.response.CurrentUserResponse;
import com.procurement.monitor.entity.UserAccount;
import com.procurement.monitor.repository.RoleRepository;
import com.procurement.monitor.repository.UserAccountRepository;
import com.procurement.monitor.repository.UserRoleRepository;
import com.procurement.monitor.validation.Checks;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountSecurityService {

    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;

    public AccountSecurityService(
        UserAccountRepository users,
        UserRoleRepository userRoles,
        RoleRepository roles
    ) {
        this.users = users;
        this.userRoles = userRoles;
        this.roles = roles;
    }

    public List<String> roleNames(Long userId) {
        return userRoles.findByUserId(userId).stream()
            .map(link -> roles.findById(link.getRoleId()))
            .flatMap(java.util.Optional::stream)
            .map(role -> role.getRoleName())
            .distinct()
            .sorted()
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> authenticate(
        Long userId,
        String tokenCredentialVersion
    ) {
        UserAccount user = users.findById(userId)
            .orElseThrow(() ->
                new InvalidBearerTokenException(
                    "Account is unavailable"
                )
            );

        if (!Boolean.TRUE.equals(user.getActive())
            || user.getDeletedAt() != null
            || user.getPasswordHash() == null
            || !CredentialVersion.matches(
                CredentialVersion.fromPasswordHash(
                    user.getPasswordHash()
                ),
                tokenCredentialVersion
            )) {
            throw new InvalidBearerTokenException(
                "Account or token is unavailable"
            );
        }

        return roleNames(userId);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse current(Long userId) {
        UserAccount user = Checks.found(
            users.findById(userId), "User not found"
        );

        return new CurrentUserResponse(
            user.getUserId(),
            user.getFullName(),
            user.getEmail(),
            roleNames(userId)
        );
    }
}

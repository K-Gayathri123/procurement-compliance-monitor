package com.procurement.monitor.config;

import com.procurement.monitor.repository.RoleRepository;
import com.procurement.monitor.repository.UserAccountRepository;
import com.procurement.monitor.repository.UserRoleRepository;
import com.procurement.monitor.validation.Checks;
import com.procurement.monitor.validation.PasswordRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("bootstrap")
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log =
        LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder encoder;
    private final String password;

    public AdminBootstrap(
        UserAccountRepository users,
        RoleRepository roles,
        UserRoleRepository userRoles,
        PasswordEncoder encoder,
        @Value("${app.bootstrap.password}") String password
    ) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var user = Checks.found(
            users.findByEmail("david@example.com"),
            "Seed admin david@example.com is missing"
        );

        Checks.require(
            user.getDeletedAt() == null,
            "Seed admin is soft-deleted"
        );

        var adminRole = Checks.found(
            roles.findByRoleName("ADMIN"),
            "ADMIN role is missing"
        );

        boolean isAdmin = userRoles
            .findByUserId(user.getUserId())
            .stream()
            .anyMatch(link ->
                link.getRoleId().equals(
                    adminRole.getRoleId()
                ));

        Checks.require(
            isAdmin, "Seed account is not an admin"
        );

        if (user.getPasswordHash() != null) {
            log.info(
                "Bootstrap skipped; admin password already exists"
            );
            return;
        }

        PasswordRules.validateNewPassword(password);

        user.setPasswordHash(
            encoder.encode(password)
        );
        user.setActive(true);

        users.save(user);

        log.info(
            "Seed admin activated; userId={}",
            user.getUserId()
        );
    }
}

package com.hms.seed;

import com.hms.auth.domain.Permission;
import com.hms.auth.domain.Role;
import com.hms.auth.domain.User;
import com.hms.auth.repository.PermissionRepository;
import com.hms.auth.repository.RoleRepository;
import com.hms.auth.repository.UserRepository;
import com.hms.branch.Branch;
import com.hms.branch.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permission> permissions = seedPermissions();
        seedRoles(permissions);
        Branch central = seedBranches();
        seedSuperAdmin(central);
        log.info("Data seeding complete");
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> byCode = new java.util.LinkedHashMap<>();
        PermissionCatalog.PERMISSIONS.forEach((code, name) -> {
            Permission permission = permissionRepository.findByCode(code).orElseGet(() -> {
                Permission p = Permission.builder().code(code).name(name).build();
                return permissionRepository.save(p);
            });
            byCode.put(code, permission);
        });
        return byCode;
    }

    private void seedRoles(Map<String, Permission> permissions) {
        Map<String, String> names = PermissionCatalog.roleNames();
        PermissionCatalog.ROLE_PERMISSIONS.forEach((code, permCodes) -> {
            Role role = roleRepository.findByCode(code).orElseGet(() -> {
                Role r = Role.builder().code(code).name(names.get(code)).build();
                return roleRepository.save(r);
            });
            Set<Permission> perms = new HashSet<>();
            for (String permCode : permCodes) {
                Permission p = permissions.get(permCode);
                if (p != null) {
                    perms.add(p);
                }
            }
            role.setPermissions(perms);
            roleRepository.save(role);
        });
    }

    private Branch seedBranches() {
        return branchRepository.findByCode("MAIN").orElseGet(() -> {
            Branch branch = Branch.builder()
                    .name("Main Branch")
                    .code("MAIN")
                    .central(true)
                    .active(true)
                    .build();
            Branch saved = branchRepository.save(branch);
            log.info("Created default branch: {}", saved.getName());
            return saved;
        });
    }

    private void seedSuperAdmin(Branch branch) {
        if (!userRepository.existsByUsername("admin")) {
            Role superAdmin = roleRepository.findByCode("SUPER_ADMIN").orElseThrow();
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("System Administrator")
                    .email("admin@hms.local")
                    .active(true)
                    .branchId(branch.getId())
                    .roles(Set.of(superAdmin))
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user: admin / Admin@123");
        }
    }
}
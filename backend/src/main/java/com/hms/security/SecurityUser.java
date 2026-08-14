package com.hms.security;

import com.hms.auth.domain.Permission;
import com.hms.auth.domain.Role;
import com.hms.auth.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class SecurityUser implements UserDetails {

    private final Long id;
    private final Long branchId;
    private final String username;
    private final String password;
    private final boolean active;
    private final Set<String> roleCodes;
    private final Set<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.branchId = user.getBranchId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.active = user.isActive();

        Set<String> roles = new HashSet<>();
        Set<GrantedAuthority> auths = new HashSet<>();
        for (Role role : user.getRoles()) {
            roles.add(role.getCode());
            auths.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (Permission permission : role.getPermissions()) {
                auths.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
        this.roleCodes = roles;
        this.authorities = auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
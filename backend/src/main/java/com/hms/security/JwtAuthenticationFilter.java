package com.hms.security;

import com.hms.common.BranchContext;
import com.hms.auth.domain.User;
import com.hms.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                User user = userRepository.findByUsername(claims.getSubject()).orElse(null);
                if (user != null && user.isActive()) {
                    SecurityUser securityUser = new SecurityUser(user);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    Long branchId = claims.get("br", Long.class);
                    if (branchId == null) {
                        branchId = user.getBranchId();
                    }
                    Long uid = claims.get("uid", Long.class);
                    BranchContext.set(branchId, uid != null ? uid : user.getId(), user.getUsername());
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            BranchContext.clear();
        }
    }
}
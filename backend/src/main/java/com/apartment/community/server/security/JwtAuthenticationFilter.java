package com.apartment.community.server.security;

import com.apartment.community.server.domain.AuthSession;
import com.apartment.community.server.domain.UserAccount;
import com.apartment.community.server.repository.AuthSessionRepository;
import com.apartment.community.server.repository.UserAccountRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAccountRepository users;
    private final AuthSessionRepository sessions;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository users,
                                   AuthSessionRepository sessions) {
        this.jwtService = jwtService;
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                JwtService.AccessClaims claims = jwtService.parseAccessToken(header.substring(7));
                UserAccount user = users.findById(claims.userId()).orElse(null);
                AuthSession session = sessions.findById(claims.sessionId()).orElse(null);
                if (user != null && user.enabled && session != null && session.user.getId().equals(user.getId())
                        && session.revokedAt == null && session.expiresAt.isAfter(Instant.now())) {
                    UserPrincipal principal = new UserPrincipal(user.id, user.normalizedEmail, user.displayName);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, List.of());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}

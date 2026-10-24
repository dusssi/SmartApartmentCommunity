package com.apartment.community.server.service;

import com.apartment.community.server.domain.AuthSession;
import com.apartment.community.server.domain.Community;
import com.apartment.community.server.domain.CommunityMembership;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.UserAccount;
import com.apartment.community.server.dto.AuthDtos;
import com.apartment.community.server.repository.AuthSessionRepository;
import com.apartment.community.server.repository.CommunityRepository;
import com.apartment.community.server.repository.MembershipRepository;
import com.apartment.community.server.repository.UserAccountRepository;
import com.apartment.community.server.security.JwtService;
import com.apartment.community.server.security.SecuritySupport;
import com.apartment.community.server.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final CommunityRepository communities;
    private final MembershipRepository memberships;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditRecorder audit;
    private final boolean allowRegistration;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository users, CommunityRepository communities,
                       MembershipRepository memberships, AuthSessionRepository sessions,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuditRecorder audit,
                       @Value("${app.auth.allow-registration}") boolean allowRegistration) {
        this.users = users;
        this.communities = communities;
        this.memberships = memberships;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
        this.allowRegistration = allowRegistration;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        if (!allowRegistration) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REGISTRATION_DISABLED", "Public registration is disabled.");
        }
        String email = normalizeEmail(request.email());
        if (users.findByNormalizedEmail(email).isPresent()) {
            throw ApiException.conflict("EMAIL_IN_USE", "An account with this email already exists.");
        }
        Community community = new Community();
        community.name = request.communityName().trim();
        communities.save(community);

        UserAccount user = new UserAccount();
        user.normalizedEmail = email;
        user.displayName = request.name().trim();
        user.passwordHash = passwordEncoder.encode(request.password());
        users.save(user);

        CommunityMembership membership = new CommunityMembership();
        membership.community = community;
        membership.user = user;
        membership.role = Types.Role.COMMUNITY_ADMIN;
        membership.unitLabel = trimToNull(request.unitLabel());
        memberships.save(membership);

        AccessService.AccessContext context = new AccessService.AccessContext(user, membership);
        audit.record(context, "COMMUNITY_REGISTERED", "Community", community.getId(), "Community and administrator created");
        return issueSession(user);
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = users.findByNormalizedEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            // Perform comparable password work without revealing whether the account exists.
            passwordEncoder.encode(request.password());
            throw ApiException.unauthorized("Invalid email or password.");
        }
        if (!user.enabled || !passwordEncoder.matches(request.password(), user.passwordHash)) {
            throw ApiException.unauthorized("Invalid email or password.");
        }
        return issueSession(user);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        AuthSession previous = sessions.findByRefreshTokenHash(hash(request.refreshToken())).orElse(null);
        if (previous == null || previous.revokedAt != null || !previous.expiresAt.isAfter(Instant.now())) {
            throw ApiException.unauthorized("The refresh token is invalid or expired.");
        }
        UserAccount user = users.findById(previous.user.getId()).filter(value -> value.enabled)
                .orElseThrow(() -> ApiException.unauthorized("The user session is no longer active."));
        IssuedSession issued = newSession(user);
        Instant now = Instant.now();
        previous.revokedAt = now;
        previous.lastUsedAt = now;
        previous.replacedBySessionId = issued.session().getId();
        return tokenResponse(user, issued);
    }

    @Transactional
    public void logout(AuthDtos.LogoutRequest request) {
        sessions.findByRefreshTokenHash(hash(request.refreshToken())).ifPresent(session -> {
            if (session.revokedAt == null) {
                session.revokedAt = Instant.now();
                session.lastUsedAt = session.revokedAt;
            }
        });
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserView me() {
        UserAccount user = users.findById(SecuritySupport.userId()).filter(value -> value.enabled)
                .orElseThrow(() -> ApiException.unauthorized("The user session is no longer active."));
        return userView(user);
    }

    private AuthDtos.TokenResponse issueSession(UserAccount user) {
        return tokenResponse(user, newSession(user));
    }

    private IssuedSession newSession(UserAccount user) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        AuthSession session = new AuthSession();
        session.user = user;
        session.refreshTokenHash = hash(refreshToken);
        session.expiresAt = Instant.now().plus(jwtService.refreshTokenTtl());
        sessions.save(session);
        return new IssuedSession(session, refreshToken);
    }

    private AuthDtos.TokenResponse tokenResponse(UserAccount user, IssuedSession issued) {
        String accessToken = jwtService.createAccessToken(user.getId(), issued.session().getId());
        return new AuthDtos.TokenResponse("Bearer", accessToken, issued.refreshToken(),
                jwtService.accessTokenSeconds(), userView(user));
    }

    private AuthDtos.UserView userView(UserAccount user) {
        List<AuthDtos.MembershipView> views = memberships.findByUserIdAndActiveTrueOrderByCreatedAtAsc(user.getId())
                .stream().map(value -> new AuthDtos.MembershipView(value.community.getId(),
                        value.community.getName(), value.role, value.unitLabel)).toList();
        return new AuthDtos.UserView(user.getId(), user.displayName, user.normalizedEmail, views);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable.", error);
        }
    }

    private record IssuedSession(AuthSession session, String refreshToken) { }
}

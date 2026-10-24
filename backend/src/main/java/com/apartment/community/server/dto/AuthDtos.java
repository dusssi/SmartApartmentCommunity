package com.apartment.community.server.dto;

import com.apartment.community.server.domain.Types;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 120) String communityName,
            @Size(max = 60) String unitLabel) { }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password) { }

    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) { }
    public record LogoutRequest(@NotBlank @Size(max = 512) String refreshToken) { }

    public record MembershipView(String communityId, String communityName,
                                 Types.Role role, String unitLabel) { }

    public record UserView(String id, String name, String email,
                           List<MembershipView> memberships) { }

    public record TokenResponse(String tokenType, String accessToken, String refreshToken,
                                long expiresIn, UserView user) { }

    public record MembershipRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull Types.Role role,
            @Size(max = 60) String unitLabel) { }

    public record MembershipUpdateRequest(
            @NotNull Types.Role role,
            @NotNull Boolean active,
            @Size(max = 60) String unitLabel,
            @NotNull Long expectedVersion) { }

    public record AccessMembershipView(String membershipId, String userId, String name, String email,
                                       Types.Role role, String unitLabel, boolean active,
                                       java.time.Instant createdAt, java.time.Instant updatedAt, long version) { }
}

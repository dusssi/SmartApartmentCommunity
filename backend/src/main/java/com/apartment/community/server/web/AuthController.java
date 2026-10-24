package com.apartment.community.server.web;

import com.apartment.community.server.dto.AuthDtos;
import com.apartment.community.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    ResponseEntity<AuthDtos.TokenResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @PostMapping("/login")
    AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return auth.refresh(request);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
        auth.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    AuthDtos.UserView me() {
        return auth.me();
    }
}

package com.remittance.user.api;

import com.remittance.user.api.dto.AuthResponse;
import com.remittance.user.api.dto.LoginRequest;
import com.remittance.user.api.dto.RegisterRequest;
import com.remittance.user.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(
                request.email(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(result.userId(), result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(result.userId(), result.accessToken(), result.refreshToken()));
    }
}

package com.fabrodiego.moneyguard.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fabrodiego.moneyguard.auth.AuthDtos.LoginRequest;
import com.fabrodiego.moneyguard.auth.AuthDtos.MeResponse;
import com.fabrodiego.moneyguard.auth.AuthDtos.RegisterRequest;
import com.fabrodiego.moneyguard.auth.AuthDtos.TokenResponse;
import com.fabrodiego.moneyguard.common.NotFoundException;
import com.fabrodiego.moneyguard.user.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(Long.valueOf(jwt.getSubject()))
            .map(u -> new MeResponse(u.getId(), u.getName(), u.getEmail()))
            .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));
    }
}

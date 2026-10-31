package com.fabrodiego.moneyguard.auth;

import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.auth.AuthDtos.LoginRequest;
import com.fabrodiego.moneyguard.auth.AuthDtos.RegisterRequest;
import com.fabrodiego.moneyguard.auth.AuthDtos.TokenResponse;
import com.fabrodiego.moneyguard.common.ConflictException;
import com.fabrodiego.moneyguard.user.User;
import com.fabrodiego.moneyguard.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("E-mail ja cadastrado");
        }
        User user = users.save(new User(req.name().trim(), email, passwordEncoder.encode(req.password())));
        return tokens.issue(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email()))
            .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
            .orElseThrow(() -> new BadCredentialsException("E-mail ou senha invalidos"));
        return tokens.issue(user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

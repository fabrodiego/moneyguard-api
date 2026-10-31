package com.fabrodiego.moneyguard.common;

import org.springframework.security.oauth2.jwt.Jwt;

/** O subject do JWT e o id do usuario logado. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

package com.fabrodiego.moneyguard.discord;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fabrodiego.moneyguard.common.CurrentUser;
import com.fabrodiego.moneyguard.discord.DiscordDtos.LinkCodeResponse;
import com.fabrodiego.moneyguard.discord.DiscordDtos.LinkStatus;

/** Lado do site: gerar codigo, ver e desfazer o vinculo com o Discord. */
@RestController
@RequestMapping("/api/discord")
public class DiscordLinkController {

    private final DiscordLinkService service;

    public DiscordLinkController(DiscordLinkService service) {
        this.service = service;
    }

    @GetMapping("/link")
    public LinkStatus status(@AuthenticationPrincipal Jwt jwt) {
        return service.status(CurrentUser.id(jwt));
    }

    @PostMapping("/link-code")
    @ResponseStatus(HttpStatus.CREATED)
    public LinkCodeResponse createCode(@AuthenticationPrincipal Jwt jwt) {
        return service.createCode(CurrentUser.id(jwt));
    }

    @DeleteMapping("/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal Jwt jwt) {
        service.unlink(CurrentUser.id(jwt));
    }
}

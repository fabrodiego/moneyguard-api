package com.fabrodiego.moneyguard.discord;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.ConflictException;
import com.fabrodiego.moneyguard.discord.DiscordDtos.LinkCodeResponse;
import com.fabrodiego.moneyguard.discord.DiscordDtos.LinkStatus;

@Service
public class DiscordLinkService {

    /** Sem 0/O, 1/I/L: facil de digitar no celular. */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    private final DiscordLinkRepository links;
    private final DiscordLinkCodeRepository codes;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public DiscordLinkService(DiscordLinkRepository links, DiscordLinkCodeRepository codes, Clock clock) {
        this.links = links;
        this.codes = codes;
        this.clock = clock;
    }

    /** Gera um codigo novo (o anterior do usuario deixa de valer). */
    @Transactional
    public LinkCodeResponse createCode(Long userId) {
        Instant now = Instant.now(clock);
        codes.deleteByUserIdOrExpired(userId, now);
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        DiscordLinkCode saved = codes.save(new DiscordLinkCode(code.toString(), userId, now.plus(CODE_TTL)));
        return new LinkCodeResponse(saved.getCode(), saved.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public LinkStatus status(Long userId) {
        return links.findById(userId)
            .map(l -> new LinkStatus(true, l.getDiscordUserId(), l.getLinkedAt()))
            .orElse(new LinkStatus(false, null, null));
    }

    @Transactional
    public void unlink(Long userId) {
        links.deleteById(userId);
    }

    /** Chamado pelo bot com o codigo digitado no /vincular. Devolve o id do usuario. */
    @Transactional
    public Long link(String discordUserId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        Instant now = Instant.now(clock);
        DiscordLinkCode found = codes.findById(code)
            .filter(c -> c.getExpiresAt().isAfter(now))
            .orElseThrow(() -> new BadRequestException("Codigo invalido ou expirado. Gere outro no site."));

        links.findByDiscordUserId(discordUserId)
            .filter(l -> !l.getUserId().equals(found.getUserId()))
            .ifPresent(l -> {
                throw new ConflictException("Este Discord ja esta vinculado a outra conta do MoneyGuard");
            });

        links.findById(found.getUserId()).ifPresent(links::delete);
        links.flush();
        links.save(new DiscordLink(found.getUserId(), discordUserId, now));
        codes.delete(found);
        return found.getUserId();
    }

    @Transactional(readOnly = true)
    public Long userIdOf(String discordUserId) {
        return links.findByDiscordUserId(discordUserId)
            .map(DiscordLink::getUserId)
            .orElseThrow(DiscordNotLinkedException::new);
    }
}

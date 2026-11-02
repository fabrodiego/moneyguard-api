package com.fabrodiego.moneyguard.bot;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fabrodiego.moneyguard.bot.BotDtos.BotTransactionRequest;
import com.fabrodiego.moneyguard.bot.BotDtos.DiscordUserRequest;
import com.fabrodiego.moneyguard.bot.BotDtos.LinkRequest;
import com.fabrodiego.moneyguard.bot.BotDtos.LinkResponse;
import com.fabrodiego.moneyguard.bot.BotDtos.ParseRequest;
import com.fabrodiego.moneyguard.bot.BotDtos.ParseResponse;
import com.fabrodiego.moneyguard.bot.BotDtos.Summary;
import com.fabrodiego.moneyguard.category.CategoryDtos.CategoryResponse;
import com.fabrodiego.moneyguard.category.CategoryService;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionResponse;
import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/** Rotas do bot do Discord. Autenticacao: header X-Bot-Key (ver BotKeyFilter). */
@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final BotService service;
    private final CategoryService categories;

    public BotController(BotService service, CategoryService categories) {
        this.service = service;
        this.categories = categories;
    }

    @PostMapping("/link")
    public LinkResponse link(@Valid @RequestBody LinkRequest req) {
        return new LinkResponse(service.link(req.discordUserId(), req.code()));
    }

    @PostMapping("/unlink")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@Valid @RequestBody DiscordUserRequest req) {
        service.unlink(req.discordUserId());
    }

    @PostMapping("/parse")
    public ParseResponse parse(@Valid @RequestBody ParseRequest req) {
        return service.parse(req.discordUserId(), req.text());
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories(@RequestParam @Pattern(regexp = BotDtos.DISCORD_ID) String discordUserId,
                                             @RequestParam(required = false) TransactionType type) {
        return categories.list(service.userId(discordUserId), type);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody BotTransactionRequest req) {
        return service.createTransaction(req);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestParam @Pattern(regexp = BotDtos.DISCORD_ID) String discordUserId) {
        service.deleteTransaction(discordUserId, id);
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam @Pattern(regexp = BotDtos.DISCORD_ID) String discordUserId) {
        return service.summary(discordUserId);
    }
}

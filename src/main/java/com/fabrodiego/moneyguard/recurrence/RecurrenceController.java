package com.fabrodiego.moneyguard.recurrence;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fabrodiego.moneyguard.common.CurrentUser;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceRequest;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceResponse;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceSummary;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recurrences")
public class RecurrenceController {

    private final RecurrenceService service;

    public RecurrenceController(RecurrenceService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecurrenceResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.id(jwt));
    }

    @GetMapping("/summary")
    public RecurrenceSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return service.summary(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public RecurrenceResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurrenceResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RecurrenceRequest req) {
        return service.create(CurrentUser.id(jwt), req);
    }

    @PutMapping("/{id}")
    public RecurrenceResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody RecurrenceRequest req) {
        return service.update(CurrentUser.id(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}

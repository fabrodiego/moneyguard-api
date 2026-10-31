package com.fabrodiego.moneyguard.merchant;

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
import com.fabrodiego.moneyguard.merchant.MerchantDtos.MerchantRequest;
import com.fabrodiego.moneyguard.merchant.MerchantDtos.MerchantResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService service;

    public MerchantController(MerchantService service) {
        this.service = service;
    }

    @GetMapping
    public List<MerchantResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public MerchantResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return MerchantResponse.from(service.get(CurrentUser.id(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MerchantRequest req) {
        return service.create(CurrentUser.id(jwt), req);
    }

    @PutMapping("/{id}")
    public MerchantResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody MerchantRequest req) {
        return service.update(CurrentUser.id(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}

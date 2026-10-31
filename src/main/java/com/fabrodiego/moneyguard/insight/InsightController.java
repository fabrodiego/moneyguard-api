package com.fabrodiego.moneyguard.insight;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fabrodiego.moneyguard.common.CurrentUser;
import com.fabrodiego.moneyguard.insight.InsightDtos.InsightResponse;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService service;

    public InsightController(InsightService service) {
        this.service = service;
    }

    /** Ex.: ?period=QUARTER&year=2026&number=4 (4o trimestre de 2026). Sem year/number = periodo atual. */
    @GetMapping
    public InsightResponse get(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(defaultValue = "MONTH") InsightPeriod period,
                               @RequestParam(required = false) Integer year,
                               @RequestParam(required = false) Integer number) {
        return service.get(CurrentUser.id(jwt), period, year, number);
    }
}

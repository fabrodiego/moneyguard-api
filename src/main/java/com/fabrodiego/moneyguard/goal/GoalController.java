package com.fabrodiego.moneyguard.goal;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fabrodiego.moneyguard.common.CurrentUser;
import com.fabrodiego.moneyguard.goal.GoalDtos.ContributionRequest;
import com.fabrodiego.moneyguard.goal.GoalDtos.ContributionResponse;
import com.fabrodiego.moneyguard.goal.GoalDtos.GoalRequest;
import com.fabrodiego.moneyguard.goal.GoalDtos.GoalResponse;
import com.fabrodiego.moneyguard.goal.GoalDtos.GoalSummary;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(CurrentUser.id(jwt), includeArchived);
    }

    @GetMapping("/summary")
    public GoalSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return service.summary(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public GoalResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GoalRequest req) {
        return service.create(CurrentUser.id(jwt), req);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody GoalRequest req) {
        return service.update(CurrentUser.id(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }

    @GetMapping("/{id}/contributions")
    public List<ContributionResponse> contributions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.listContributions(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContributionResponse addContribution(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                @Valid @RequestBody ContributionRequest req) {
        return service.addContribution(CurrentUser.id(jwt), id, req);
    }

    @DeleteMapping("/{id}/contributions/{contributionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContribution(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @PathVariable Long contributionId) {
        service.deleteContribution(CurrentUser.id(jwt), id, contributionId);
    }
}

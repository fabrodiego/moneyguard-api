package com.fabrodiego.moneyguard.category;

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

import com.fabrodiego.moneyguard.category.CategoryDtos.CategoryResponse;
import com.fabrodiego.moneyguard.category.CategoryDtos.CreateCategoryRequest;
import com.fabrodiego.moneyguard.category.CategoryDtos.UpdateCategoryRequest;
import com.fabrodiego.moneyguard.common.CurrentUser;
import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) TransactionType type) {
        return service.list(CurrentUser.id(jwt), type);
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return CategoryResponse.from(service.get(CurrentUser.id(jwt), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateCategoryRequest req) {
        return service.create(CurrentUser.id(jwt), req);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody UpdateCategoryRequest req) {
        return service.update(CurrentUser.id(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}

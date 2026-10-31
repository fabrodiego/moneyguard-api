package com.fabrodiego.moneyguard.category;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.category.CategoryDtos.CategoryResponse;
import com.fabrodiego.moneyguard.category.CategoryDtos.CreateCategoryRequest;
import com.fabrodiego.moneyguard.category.CategoryDtos.UpdateCategoryRequest;
import com.fabrodiego.moneyguard.common.ConflictException;
import com.fabrodiego.moneyguard.common.NotFoundException;
import com.fabrodiego.moneyguard.transaction.TransactionRepository;
import com.fabrodiego.moneyguard.transaction.TransactionType;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final TransactionRepository transactions;

    public CategoryService(CategoryRepository categories, TransactionRepository transactions) {
        this.categories = categories;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId, TransactionType type) {
        List<Category> found = type == null
            ? categories.findByUserIdOrderByTypeAscNameAsc(userId)
            : categories.findByUserIdAndTypeOrderByNameAsc(userId, type);
        return found.stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Category get(Long userId, Long id) {
        return categories.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Categoria nao encontrada"));
    }

    @Transactional
    public CategoryResponse create(Long userId, CreateCategoryRequest req) {
        String name = req.name().trim();
        if (categories.existsDuplicate(userId, name, req.type(), null)) {
            throw new ConflictException("Ja existe uma categoria com esse nome e tipo");
        }
        return CategoryResponse.from(categories.save(new Category(userId, name, req.type(), req.color())));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, UpdateCategoryRequest req) {
        Category category = get(userId, id);
        String name = req.name().trim();
        if (categories.existsDuplicate(userId, name, category.getType(), id)) {
            throw new ConflictException("Ja existe uma categoria com esse nome e tipo");
        }
        category.update(name, req.color());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Category category = get(userId, id);
        if (transactions.existsByCategoryId(category.getId())) {
            throw new ConflictException("Categoria possui lancamentos; mova-os antes de excluir");
        }
        categories.delete(category);
    }
}

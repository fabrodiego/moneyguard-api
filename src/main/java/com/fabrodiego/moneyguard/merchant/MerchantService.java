package com.fabrodiego.moneyguard.merchant;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.category.CategoryService;
import com.fabrodiego.moneyguard.common.ConflictException;
import com.fabrodiego.moneyguard.common.NotFoundException;
import com.fabrodiego.moneyguard.common.TextNormalizer;
import com.fabrodiego.moneyguard.merchant.MerchantDtos.MerchantRequest;
import com.fabrodiego.moneyguard.merchant.MerchantDtos.MerchantResponse;

@Service
public class MerchantService {

    private final MerchantRepository merchants;
    private final CategoryService categories;

    public MerchantService(MerchantRepository merchants, CategoryService categories) {
        this.merchants = merchants;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> list(Long userId) {
        return merchants.findByUserIdOrderByNameAsc(userId).stream().map(MerchantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Merchant get(Long userId, Long id) {
        return merchants.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Empresa nao encontrada"));
    }

    /** Busca pelo nome normalizado (sem acento, minusculo). Usado pelo bot. */
    @Transactional(readOnly = true)
    public Optional<Merchant> findByName(Long userId, String name) {
        return merchants.findByUserIdAndNormalizedName(userId, TextNormalizer.normalize(name));
    }

    @Transactional
    public MerchantResponse create(Long userId, MerchantRequest req) {
        String normalized = TextNormalizer.normalize(req.name());
        ensureUnique(userId, normalized, null);
        Category category = categories.get(userId, req.categoryId());
        return MerchantResponse.from(merchants.save(new Merchant(userId, req.name().trim(), normalized, category)));
    }

    @Transactional
    public MerchantResponse update(Long userId, Long id, MerchantRequest req) {
        Merchant merchant = get(userId, id);
        String normalized = TextNormalizer.normalize(req.name());
        ensureUnique(userId, normalized, id);
        merchant.update(req.name().trim(), normalized, categories.get(userId, req.categoryId()));
        return MerchantResponse.from(merchant);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        merchants.delete(get(userId, id));
    }

    private void ensureUnique(Long userId, String normalized, Long excludeId) {
        merchants.findByUserIdAndNormalizedName(userId, normalized)
            .filter(m -> !m.getId().equals(excludeId))
            .ifPresent(m -> {
                throw new ConflictException("Ja existe uma empresa com esse nome");
            });
    }
}

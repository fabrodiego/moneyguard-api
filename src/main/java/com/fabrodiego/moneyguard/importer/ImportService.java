package com.fabrodiego.moneyguard.importer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.category.CategoryService;
import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.ConflictException;
import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.common.TextNormalizer;
import com.fabrodiego.moneyguard.crypto.CryptoService;
import com.fabrodiego.moneyguard.importer.ImportDtos.ApproveRequest;
import com.fabrodiego.moneyguard.importer.ImportDtos.PreviewItem;
import com.fabrodiego.moneyguard.importer.ImportDtos.PreviewResponse;
import com.fabrodiego.moneyguard.merchant.Merchant;
import com.fabrodiego.moneyguard.merchant.MerchantAlias;
import com.fabrodiego.moneyguard.merchant.MerchantAliasRepository;
import com.fabrodiego.moneyguard.merchant.MerchantDtos.MerchantResponse;
import com.fabrodiego.moneyguard.merchant.MerchantRepository;
import com.fabrodiego.moneyguard.merchant.MerchantService;
import com.fabrodiego.moneyguard.transaction.Transaction;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionResponse;
import com.fabrodiego.moneyguard.transaction.TransactionRepository;
import com.fabrodiego.moneyguard.transaction.TransactionType;
import com.fabrodiego.moneyguard.user.UserRepository;

/**
 * Importacao em duas etapas: preview le o arquivo e sugere empresa/categoria sem gravar nada;
 * approve grava uma linha por vez. Linhas puladas nao deixam rastro.
 */
@Service
public class ImportService {

    private final InterStatementParser statementParser;
    private final InterCardBillParser billParser;
    private final TransactionRepository transactions;
    private final MerchantRepository merchants;
    private final MerchantAliasRepository aliases;
    private final MerchantService merchantService;
    private final CategoryService categories;
    private final UserRepository users;
    private final CryptoService crypto;

    public ImportService(InterStatementParser statementParser, InterCardBillParser billParser,
                         TransactionRepository transactions, MerchantRepository merchants,
                         MerchantAliasRepository aliases, MerchantService merchantService,
                         CategoryService categories, UserRepository users, CryptoService crypto) {
        this.statementParser = statementParser;
        this.billParser = billParser;
        this.transactions = transactions;
        this.merchants = merchants;
        this.aliases = aliases;
        this.merchantService = merchantService;
        this.categories = categories;
        this.users = users;
        this.crypto = crypto;
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(Long userId, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("Arquivo vazio");
        }
        ParsedFile parsed = InterCardBillParser.indexOf(content, "%PDF".getBytes()) >= 0
            ? billParser.parse(content)
            : statementParser.parse(content);

        Suggester suggester = new Suggester(merchants.findByUserIdOrderByNameAsc(userId), aliases.findByUserId(userId));
        String ownName = users.findById(userId).map(u -> words(u.getName())).orElse("");

        List<String> keys = keys(userId, parsed);
        Set<String> existing = keys.isEmpty() ? Set.of()
            : new HashSet<>(transactions.findExistingImportKeys(userId, keys));

        List<PreviewItem> items = new ArrayList<>();
        for (int i = 0; i < parsed.entries().size(); i++) {
            ParsedFile.Entry e = parsed.entries().get(i);
            String installment = e.installment() == null ? null : e.installment() + "/" + e.installments();
            String suggestedDescription = installment == null ? e.description() : e.description() + " (" + installment + ")";
            Merchant merchant = suggester.find(e.description()).orElse(null);
            MerchantResponse suggestion = merchant == null || merchant.getCategory().getType() != e.type()
                ? null : MerchantResponse.from(merchant);
            items.add(new PreviewItem(keys.get(i), e.date(), e.description(), suggestedDescription, e.amount(),
                e.type(), e.history(), installment, isOwnTransfer(e, ownName), existing.contains(keys.get(i)),
                suggestion));
        }
        return new PreviewResponse(parsed.kind(), items, parsed.ignored(), parsed.declaredTotal(),
            parsed.parsedTotal(), warning(parsed));
    }

    @Transactional
    public TransactionResponse approve(Long userId, ApproveRequest req) {
        Category category = categories.get(userId, req.categoryId());
        if (category.getType() != req.type()) {
            throw new BadRequestException(req.type() == TransactionType.EXPENSE
                ? "Escolha uma categoria de gasto" : "Escolha uma categoria de renda");
        }
        if (transactions.existsByUserIdAndImportKey(userId, req.importKey())) {
            throw new ConflictException("Esse lancamento ja foi importado");
        }
        Merchant merchant = req.merchantId() == null ? null : merchantService.get(userId, req.merchantId());
        String description = req.description() == null || req.description().isBlank() ? null : req.description().trim();
        Transaction saved = transactions.save(Transaction.fromImport(userId, category, merchant,
            Money.of(req.amount()), description, req.occurredOn(), req.importKey()));
        if (merchant != null && req.alias() != null && !req.alias().isBlank()) {
            learnAlias(userId, merchant, req.alias());
        }
        return TransactionResponse.from(saved);
    }

    private void learnAlias(Long userId, Merchant merchant, String alias) {
        String normalized = TextNormalizer.normalize(alias);
        if (normalized.length() > MerchantAlias.MAX_LENGTH) {
            normalized = normalized.substring(0, MerchantAlias.MAX_LENGTH);
        }
        Optional<MerchantAlias> current = aliases.findByUserIdAndNormalizedAlias(userId, normalized);
        if (current.isPresent()) {
            current.get().pointTo(merchant);
        } else {
            aliases.save(new MerchantAlias(userId, merchant, normalized));
        }
    }

    /**
     * Mesma linha no mesmo arquivo (ou num extrato de periodo sobreposto) gera a mesma chave.
     * Linhas identicas no mesmo dia recebem um contador para nao virarem uma so.
     */
    private List<String> keys(Long userId, ParsedFile parsed) {
        Map<String, Integer> seen = new HashMap<>();
        List<String> keys = new ArrayList<>();
        for (ParsedFile.Entry e : parsed.entries()) {
            String base = String.join("|", userId.toString(), parsed.kind().name(), e.date().toString(),
                e.type().name(), e.amount().toPlainString(), TextNormalizer.normalize(e.description()),
                String.valueOf(e.installment()), String.valueOf(e.installments()));
            int n = seen.merge(base, 1, Integer::sum);
            keys.add(crypto.fingerprint(base + "|" + n));
        }
        return keys;
    }

    private static boolean isOwnTransfer(ParsedFile.Entry e, String ownName) {
        return e.history() != null && !ownName.isEmpty()
            && TextNormalizer.normalize(e.history()).startsWith("pix")
            && words(e.description()).startsWith(ownName);
    }

    private static String warning(ParsedFile parsed) {
        BigDecimal declared = parsed.declaredTotal();
        if (declared == null || parsed.parsedTotal() == null || declared.compareTo(parsed.parsedTotal()) == 0) {
            return null;
        }
        return "A soma das compras lidas (R$ " + parsed.parsedTotal().toPlainString()
            + ") nao bate com o total da fatura (R$ " + declared.toPlainString()
            + "). Pode ter faltado alguma linha; confira antes de aprovar.";
    }

    /** "Uber *TRIP HELP.U" -> "uber trip help u" (so letras e numeros, para comparar por palavra). */
    static String words(String text) {
        return TextNormalizer.normalize(text).replaceAll("[^a-z0-9]+", " ").strip();
    }

    /** Apelido exato primeiro; senao, a empresa cujo nome aparece inteiro no texto do banco (a mais longa). */
    static final class Suggester {

        private final Map<String, Merchant> byAlias = new HashMap<>();
        private final List<Map.Entry<String, Merchant>> byName = new ArrayList<>();

        Suggester(List<Merchant> merchants, List<MerchantAlias> aliases) {
            for (MerchantAlias a : aliases) {
                byAlias.put(a.getNormalizedAlias(), a.getMerchant());
            }
            for (Merchant m : merchants) {
                String name = words(m.getName());
                if (!name.isEmpty()) {
                    byName.add(Map.entry(name, m));
                }
            }
            byName.sort(Comparator.comparingInt((Map.Entry<String, Merchant> en) -> en.getKey().length()).reversed());
        }

        Optional<Merchant> find(String description) {
            String normalized = TextNormalizer.normalize(description);
            Merchant alias = byAlias.get(normalized.length() > MerchantAlias.MAX_LENGTH
                ? normalized.substring(0, MerchantAlias.MAX_LENGTH) : normalized);
            if (alias != null) {
                return Optional.of(alias);
            }
            String haystack = " " + words(description) + " ";
            return byName.stream().filter(en -> haystack.contains(" " + en.getKey() + " "))
                .map(Map.Entry::getValue).findFirst();
        }
    }
}

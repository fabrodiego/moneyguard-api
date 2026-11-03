package com.fabrodiego.moneyguard.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fabrodiego.moneyguard.bot.MessageParser.CategoryRef;
import com.fabrodiego.moneyguard.bot.MessageParser.MerchantRef;
import com.fabrodiego.moneyguard.bot.MessageParser.Parsed;
import com.fabrodiego.moneyguard.recurrence.Frequency;
import com.fabrodiego.moneyguard.transaction.TransactionType;

class MessageParserTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    static final CategoryRef FOOD = new CategoryRef(1L, "Alimentação", TransactionType.EXPENSE);
    static final CategoryRef FUN = new CategoryRef(2L, "Lazer", TransactionType.EXPENSE);
    static final CategoryRef HOUSE = new CategoryRef(3L, "Contas da casa", TransactionType.EXPENSE);
    static final CategoryRef SALARY = new CategoryRef(4L, "Salário", TransactionType.INCOME);
    static final CategoryRef TRANSPORT = new CategoryRef(5L, "Transporte", TransactionType.EXPENSE);
    static final List<CategoryRef> CATEGORIES = List.of(FOOD, FUN, HOUSE, SALARY, TRANSPORT);
    static final List<MerchantRef> MERCHANTS = List.of(
        new MerchantRef(10L, "Cantina Central", FOOD),
        new MerchantRef(11L, "Táxi Rápido", TRANSPORT),
        new MerchantRef(12L, "Mercado São João", FOOD));

    static Parsed parse(String text) {
        return MessageParser.parse(text, CATEGORIES, MERCHANTS, TODAY);
    }

    @Test
    void exemploDoDiego() {
        Parsed p = parse("acabei de gastar 8 reais na cantina central");
        assertThat(p.amount()).isEqualByComparingTo("8.00");
        assertThat(p.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(p.merchant().name()).isEqualTo("Cantina Central");
        assertThat(p.category()).isEqualTo(FOOD);
        assertThat(p.occurredOn()).isEqualTo(TODAY);
        assertThat(p.description()).isNull();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "gastei 8,50 na Cantina Central            | 8.50",
        "R$ 12.90 no taxi rapido                 | 12.90",
        "paguei r$1.200,00 de aluguel     | 1200.00",
        "comprei 1.500 de mercado         | 1500.00",
        "torrei 2 mil no cinema           | 2000.00",
        "gastei 30 contos no bar          | 30.00",
        "almoço 23                        | 23.00",
    })
    void valores(String text, String expected) {
        assertThat(parse(text).amount()).isEqualByComparingTo(expected);
    }

    @Test
    void merchantComAcentoEVariosNomes() {
        Parsed p = parse("gastei 150 no mercado sao joao ontem");
        assertThat(p.merchant().id()).isEqualTo(12L);
        assertThat(p.occurredOn()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void rendaPorPalavraChave() {
        Parsed p = parse("recebi 6.500 de salário");
        assertThat(p.type()).isEqualTo(TransactionType.INCOME);
        assertThat(p.amount()).isEqualByComparingTo("6500");
        assertThat(p.category()).isEqualTo(SALARY);
        assertThat(p.description()).isNull();
    }

    @Test
    void categoriaPeloNomeEDescricao() {
        Parsed p = parse("gastei 45 com lazer");
        assertThat(p.category()).isEqualTo(FUN);
        assertThat(p.description()).isNull();

        Parsed q = parse("paguei 120,50 de conta de luz");
        assertThat(q.category()).isNull();
        assertThat(q.description()).isEqualTo("Luz");
    }

    @Test
    void descricaoMantemAcentos() {
        Parsed p = parse("gastei 32 no açougue");
        assertThat(p.category()).isNull();
        assertThat(p.description()).isEqualTo("Açougue");
    }

    @Test
    void datas() {
        assertThat(parse("gastei 10 no taxi rapido anteontem").occurredOn()).isEqualTo(TODAY.minusDays(2));
        assertThat(parse("gastei 10 no taxi rapido dia 1").occurredOn()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(parse("gastei 10 no taxi rapido dia 28").occurredOn()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(parse("gastei 10 no taxi rapido 15/10").occurredOn()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(parse("gastei 10 no taxi rapido 25/12").occurredOn()).isEqualTo(LocalDate.of(2025, 12, 25));
    }

    @Test
    void dataNaoViraValor() {
        Parsed p = parse("dia 15 gastei 40 reais no taxi rapido");
        assertThat(p.amount()).isEqualByComparingTo("40");
        assertThat(p.occurredOn()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void semValor() {
        assertThat(parse("gastei na cantina central").amount()).isNull();
        assertThat(parse("").amount()).isNull();
    }

    @Test
    void lancamentoAvulsoNaoEAssinatura() {
        assertThat(parse("acabei de gastar 8 reais na cantina central").recurrence()).isNull();
        assertThat(parse("gastei 10 no taxi rapido dia 28").recurrence()).isNull();
    }

    @Test
    void assinaturaMensalComDia() {
        Parsed p = parse("assinei streaming 55,90 todo dia 2");
        assertThat(p.recurrence()).isEqualTo(new MessageParser.RecurrenceHint(Frequency.MONTHLY, 2, null));
        assertThat(p.amount()).isEqualByComparingTo("55.90");
        assertThat(p.description()).isEqualTo("Streaming");
        assertThat(p.type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void assinaturaMensalSemDiaUsaHoje() {
        Parsed p = parse("academia 99,90 por mês");
        assertThat(p.recurrence()).isEqualTo(new MessageParser.RecurrenceHint(Frequency.MONTHLY, 2, null));
        assertThat(p.description()).isEqualTo("Academia");
        assertThat(p.amount()).isEqualByComparingTo("99.90");
    }

    @Test
    void rendaFixa() {
        Parsed p = parse("salário 6.500 todo dia 5");
        assertThat(p.recurrence().dayOfMonth()).isEqualTo(5);
        assertThat(p.type()).isEqualTo(TransactionType.INCOME);
        assertThat(p.category()).isEqualTo(SALARY);
        assertThat(p.amount()).isEqualByComparingTo("6500");
    }

    @Test
    void assinaturaAnualComData() {
        Parsed p = parse("domínio 120 reais todo ano em 15/03");
        assertThat(p.recurrence()).isEqualTo(new MessageParser.RecurrenceHint(Frequency.YEARLY, 15, 3));
        assertThat(p.amount()).isEqualByComparingTo("120");
        assertThat(p.description()).isEqualTo("Domínio");
    }

    @Test
    void assinaturaAnualSemDataEMerchant() {
        Parsed p = parse("assinatura anual da cantina central 300");
        assertThat(p.recurrence()).isEqualTo(new MessageParser.RecurrenceHint(Frequency.YEARLY, 2, 11));
        assertThat(p.merchant().name()).isEqualTo("Cantina Central");
        assertThat(p.amount()).isEqualByComparingTo("300");
    }

    @Test
    void mensalidadeComDiaSolto() {
        Parsed p = parse("mensalidade do curso 250 dia 10");
        assertThat(p.recurrence()).isEqualTo(new MessageParser.RecurrenceHint(Frequency.MONTHLY, 10, null));
        assertThat(p.description()).isEqualTo("Curso");
        assertThat(p.amount()).isEqualByComparingTo("250");
    }

    @Test
    void descricaoSemPreposicao() {
        assertThat(parse("almoço 23").description()).isEqualTo("Almoço");
    }
}

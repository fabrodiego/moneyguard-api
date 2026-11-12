package com.fabrodiego.moneyguard.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fabrodiego.moneyguard.transaction.TransactionType;

class InterParsersTest {

    private final InterStatementParser statement = new InterStatementParser();
    private final InterCardBillParser bill = new InterCardBillParser();

    @Test
    void statementReadsRowsAndDropsBillPayment() {
        ParsedFile f = statement.parse(InterFixtures.statementBytes());

        assertThat(f.kind()).isEqualTo(ImportKind.STATEMENT);
        assertThat(f.entries()).hasSize(7);
        assertThat(f.ignored()).singleElement().satisfies(i -> {
            assertThat(i.reason()).isEqualTo("Pagamento de fatura");
            assertThat(i.amount()).isEqualByComparingTo("1580.92");
        });

        ParsedFile.Entry pix = f.entries().get(0);
        assertThat(pix.date()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(pix.description()).isEqualTo("Fulano De Tal");
        assertThat(pix.history()).isEqualTo("Pix enviado");
        assertThat(pix.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(pix.amount()).isEqualByComparingTo("12.00");

        assertThat(f.entries().get(2).description()).isEqualTo("Padaria Pao Quente Cidade Exemplo Bra");

        ParsedFile.Entry salary = f.entries().get(5);
        assertThat(salary.description()).isEqualTo("Salário recebido - Portabilidade");
        assertThat(salary.type()).isEqualTo(TransactionType.INCOME);
        assertThat(salary.amount()).isEqualByComparingTo("3649.06");
    }

    @Test
    void statementAcceptsLatin1() {
        ParsedFile f = statement.parse(InterFixtures.STATEMENT.getBytes(StandardCharsets.ISO_8859_1));
        assertThat(f.entries()).hasSize(7);
    }

    @Test
    void statementRejectsUnknownFile() {
        assertThatThrownBy(() -> statement.parse("a;b;c\n1;2;3".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(ImportFormatException.class);
    }

    @Test
    void billReadsPurchasesInstallmentsAndCredits() {
        ParsedFile f = bill.parseText(InterFixtures.BILL_TEXT);

        assertThat(f.kind()).isEqualTo(ImportKind.CARD_BILL);
        assertThat(f.entries()).hasSize(8);
        assertThat(f.entries()).allMatch(e -> e.type() == TransactionType.EXPENSE);
        assertThat(f.entries()).noneMatch(e -> e.description().contains("NAO DEVE ENTRAR"));

        ParsedFile.Entry first = f.entries().get(0);
        assertThat(first.description()).isEqualTo("LOJA EXEMPLO CENTRO");
        assertThat(first.installment()).isEqualTo(2);
        assertThat(first.installments()).isEqualTo(2);
        // parcela 2 da compra de 23/09 cai em 23/10
        assertThat(first.date()).isEqualTo(LocalDate.of(2026, 10, 23));

        ParsedFile.Entry kabum = f.entries().get(5);
        assertThat(kabum.description()).isEqualTo("MLP *Loja-Online");
        assertThat(kabum.date()).isEqualTo(LocalDate.of(2026, 10, 29));

        assertThat(f.entries().get(2).description()).isEqualTo("Uber UBER *TRIP HELP.U");
        assertThat(f.entries().get(7).installment()).isNull();

        assertThat(f.ignored()).extracting(ParsedFile.Ignored::reason)
            .containsExactly("Pagamento da fatura", "Estorno / credito");
        assertThat(f.declaredTotal()).isEqualByComparingTo("516.40");
        // 59,90 + 6 + 10,94 + 6,89 + 15,12 + 289,92 + 77,33 + 90,30 - 20 (estorno)
        assertThat(f.parsedTotal()).isEqualByComparingTo("536.40");
    }

    @Test
    void billFromRealPdfEvenWithGarbageBeforeHeader() throws Exception {
        ParsedFile clean = bill.parse(InterFixtures.billPdf(0));
        ParsedFile padded = bill.parse(InterFixtures.billPdf(4096));
        assertThat(clean.entries()).hasSize(8);
        assertThat(padded.entries()).isEqualTo(clean.entries());
    }

    @Test
    void billRejectsOtherPdfsAndNonPdf() {
        assertThatThrownBy(() -> bill.parseText("Um PDF qualquer\nsem compras"))
            .isInstanceOf(ImportFormatException.class);
        assertThatThrownBy(() -> bill.parse("nao sou pdf".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(ImportFormatException.class);
    }
}

package com.fabrodiego.moneyguard.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Arquivos no formato do Inter, com dados ficticios. */
final class InterFixtures {

    private InterFixtures() {
    }

    static final String STATEMENT = """
         Extrato Conta Corrente\s
        Conta ;000000000
        Período ;01/10/2026 a 31/10/2026
        Saldo ;100,00

        Data Lançamento;Histórico;Descrição;Valor;Saldo
        29/10/2026;Pix enviado ;Fulano De Tal;-12,00;101,45
        23/10/2026;Crédito Evento B3;* Prov * Rendimento 1                  Abcd11;0,81;113,45
        18/10/2026;Compra no débito;Padaria Pao Quente    Cidade Exemplo Bra;-5,90;221,91
        18/10/2026;Compra no débito;Padaria Pao Quente    Cidade Exemplo Bra;-5,90;227,81
        16/10/2026;Pix recebido;Teste Da Silva;100,00;233,71
        06/10/2026;Salário recebido - Portabilidade;;3.649,06;404,91
        06/10/2026;Pagamento efetuado;Pagamento Fatura - TESTE DA SILVA;-1.580,92;-3.244,15
        06/10/2026;Pagamento de Título;OPERADORA EXEMPLO SA;-1.293,64;-1.364,64
        """;

    /** Linhas como o texto extraido do PDF da fatura (3 cartoes, parcelas, pagamento, proxima fatura). */
    static final List<String> BILL_LINES = List.of(
        "Resumo da fatura",
        "DESPESAS DO MÊS VALOR ANTECIPADO FATURA ATUAL",
        "R$ 1.000,00 R$ 0,00 R$ 1.000,00",
        "Descritivo detalhado",
        "Despesas do mês R$ 516,40",
        "Despesas da fatura",
        "CARTÃO 0000****1111",
        "Data Movimentação Beneficiário Valor",
        "23 de set. 2026 LOJA EXEMPLO CENTRO (Parcela 02 de 02) - R$ 59,90",
        "05 de out. 2026 RESTAURANTE MODELO - R$ 6,00",
        "06 de out. 2026 PAGAMENTO ON LINE - + R$ 1.580,92",
        "17 de out. 2026 Uber UBER *TRIP HELP.U - R$ 10,94",
        "21 de out. 2026 MERCADO FICTICIO - R$ 6,89",
        "21 de out. 2026 MERCADO FICTICIO - R$ 15,12",
        "22 de out. 2026 LOJA ESTORNADA - + R$ 20,00",
        "Total CARTÃO 0000****1111 R$ 78,85",
        "CARTÃO 0000****2222",
        "Data Movimentação Beneficiário Valor",
        "29 de mai. 2026 MLP *Loja-Online (Parcela 06 de 07) - R$ 289,92",
        "27 de jul. 2026 MARKETPLACE BR (Parcela 04 de 09) - R$ 77,33",
        "01 de nov. 2026 STREAMING.COM/BILL - R$ 90,30",
        "Próxima fatura",
        "MLP *Loja-Online (Parcela 07 de 07) R$ 289,92",
        "15 de nov. 2026 NAO DEVE ENTRAR - R$ 999,99");

    static final String BILL_TEXT = String.join("\n", BILL_LINES);

    static byte[] statementBytes() {
        return STATEMENT.getBytes(StandardCharsets.UTF_8);
    }

    /** Gera um PDF de verdade com as linhas, para passar pela extracao do PDFBox. */
    static byte[] billPdf(int zeroPrefix) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 760;
                for (String line : BILL_LINES) {
                    cs.beginText();
                    cs.setFont(font, 9);
                    cs.newLineAtOffset(40, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 14;
                }
            }
            out.write(new byte[zeroPrefix]);
            doc.save(out);
            return out.toByteArray();
        }
    }
}

package br.com.motoservices.util;

import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.Precificacao;
import br.com.motoservices.model.ServicoDaOficina;
import br.com.motoservices.model.ServicoTrocaPeca;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gera um comprovante interno imprimível (HTML) da OS. Não tem validade fiscal.
 */
public final class ComprovanteRenderer {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ComprovanteRenderer() {
    }

    public static String render(OrdemDeServico os) {
        StringBuilder linhas = new StringBuilder();
        BigDecimal totalPecas = Precificacao.arredondar(valor(os.getTotalPecas()));
        BigDecimal totalMaoDeObra = Precificacao.arredondar(valor(os.getTotalMaoDeObra()));
        BigDecimal desconto = Precificacao.arredondar(valor(os.getDescontoAplicado()));
        BigDecimal totalFinal = Precificacao.arredondar(valor(os.getTotalFinal()));

        for (ServicoDaOficina servico : os.getServicos()) {
            BigDecimal peca = servico instanceof ServicoTrocaPeca troca
                    ? troca.valorDaPecaDaLoja() : Precificacao.zero();
            BigDecimal maoDeObra = Precificacao.maoDeObraComFator(
                    servico.getValorBaseMaoDeObra(), os.getMoto());
            BigDecimal total = Precificacao.arredondar(peca.add(maoDeObra));
            linhas.append("<tr><td>").append(esc(servico.getDescricao()))
                    .append("</td><td>").append(tipo(servico))
                    .append("</td><td class=\"valor\">").append(moeda(peca))
                    .append("</td><td class=\"valor\">").append(moeda(maoDeObra))
                    .append("</td><td class=\"valor\">").append(moeda(total))
                    .append("</td></tr>");
        }

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Comprovante OS %d — MotoServices</title>
                  <style>
                    :root { color-scheme: light; }
                    * { box-sizing: border-box; }
                    body { margin: 0; padding: 32px; background: #f7f6f3; color: #1c1917;
                           font-family: Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif; }
                    .folha { max-width: 820px; margin: 0 auto; background: #fff; border: 1px solid #d6d3d1;
                             border-radius: 20px; padding: 40px; }
                    header { display: flex; justify-content: space-between; gap: 24px;
                             border-bottom: 1px solid #e7e5e4; padding-bottom: 24px; }
                    .marca { display: flex; align-items: center; gap: 12px; }
                    .logo { width: 40px; height: 40px; border-radius: 12px; background: #1d4d3b;
                            color: #fff; display: grid; place-items: center; font-weight: 700; }
                    h1 { font-size: 22px; margin: 0 0 4px; }
                    .muted { color: #57534e; font-size: 13px; margin: 0; }
                    h2 { font-size: 13px; text-transform: uppercase; letter-spacing: .08em;
                         color: #57534e; margin: 28px 0 8px; }
                    .grade { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
                    .bloco p { margin: 2px 0; font-size: 14px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 8px; font-size: 14px; }
                    th { text-align: left; font-size: 12px; text-transform: uppercase; letter-spacing: .05em;
                         color: #57534e; border-bottom: 1px solid #d6d3d1; padding: 8px 6px; }
                    td { padding: 10px 6px; border-bottom: 1px solid #f1efe9; vertical-align: top; }
                    .valor { text-align: right; white-space: nowrap; font-variant-numeric: tabular-nums; }
                    .totais { margin-left: auto; width: min(320px, 100%%); margin-top: 20px; }
                    .totais div { display: flex; justify-content: space-between; padding: 5px 0; font-size: 14px; }
                    .totais .final { border-top: 1px solid #d6d3d1; margin-top: 6px; padding-top: 12px;
                                     font-size: 20px; font-weight: 700; }
                    .pagamento { margin-top: 24px; padding: 16px; border-radius: 12px; background: #f1efe9;
                                 font-size: 14px; }
                    footer { margin-top: 28px; padding-top: 20px; border-top: 1px solid #e7e5e4;
                             color: #57534e; font-size: 12px; }
                    .acoes { max-width: 820px; margin: 0 auto 16px; display: flex; justify-content: flex-end; }
                    button { border: 0; border-radius: 999px; background: #1d4d3b; color: #fff;
                             padding: 10px 18px; font: inherit; font-weight: 600; cursor: pointer; }
                    @media print {
                      body { padding: 0; background: #fff; }
                      .folha { border: 0; border-radius: 0; padding: 0; max-width: none; }
                      .acoes { display: none; }
                    }
                  </style>
                </head>
                <body>
                  <div class="acoes"><button onclick="window.print()">Imprimir</button></div>
                  <main class="folha">
                    <header>
                      <div class="marca">
                        <span class="logo" aria-hidden="true">M</span>
                        <div>
                          <h1>MotoServices</h1>
                          <p class="muted">Comprovante interno de serviço</p>
                        </div>
                      </div>
                      <div style="text-align:right">
                        <strong>OS #%d</strong>
                        <p class="muted">Fechada em %s</p>
                      </div>
                    </header>

                    <div class="grade">
                      <div class="bloco">
                        <h2>Cliente</h2>
                        <p><strong>%s</strong></p>
                        <p>%s</p>
                        <p>%s</p>
                      </div>
                      <div class="bloco">
                        <h2>Moto</h2>
                        <p><strong>%s %s</strong></p>
                        <p>%d cc</p>
                      </div>
                    </div>

                    <h2>Serviços</h2>
                    <table>
                      <thead><tr><th>Descrição</th><th>Tipo</th><th class="valor">Peça</th>
                      <th class="valor">Mão de obra</th><th class="valor">Total</th></tr></thead>
                      <tbody>%s</tbody>
                    </table>

                    <div class="totais">
                      <div><span>Peças</span><span class="valor">%s</span></div>
                      <div><span>Mão de obra</span><span class="valor">%s</span></div>
                      <div><span>Desconto</span><span class="valor">− %s</span></div>
                      <div class="final"><span>Total</span><span class="valor">%s</span></div>
                    </div>

                    <div class="pagamento">
                      <strong>Forma de pagamento:</strong> %s
                    </div>

                    <footer>
                      Documento sem validade fiscal, gerado pelo MotoServices para conferência do serviço.
                      Próxima revisão sugerida: %s.
                    </footer>
                  </main>
                </body>
                </html>
                """.formatted(
                os.getId(),
                os.getId(),
                os.getDataFechamento() == null ? "—" : os.getDataFechamento().format(DATA_HORA),
                esc(os.getCliente().getNome()),
                esc(formatarDocumento(os.getCliente().getCpfCnpj())),
                esc(os.getCliente().getContato()),
                esc(os.getMoto().getMarca()),
                esc(os.getMoto().getModelo()),
                os.getMoto().getCilindradas(),
                linhas,
                moeda(totalPecas),
                moeda(totalMaoDeObra),
                moeda(desconto),
                moeda(totalFinal),
                os.getFormaPagamento() == null ? "Não informada" : os.getFormaPagamento().name(),
                os.getCliente().getProximaRevisao() == null
                        ? "—" : os.getCliente().getProximaRevisao().format(DATA));
    }

    private static BigDecimal valor(BigDecimal valor) {
        return valor == null ? Precificacao.zero() : valor;
    }

    private static String tipo(ServicoDaOficina servico) {
        return servico instanceof ServicoTrocaPeca ? "Troca de peça" : "Revisão";
    }

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    private static String formatarDocumento(String documento) {
        String digitos = documento == null ? "" : documento.replaceAll("\\D", "");
        if (digitos.length() == 11) {
            return digitos.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (digitos.length() == 14) {
            return digitos.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return documento == null ? "—" : documento;
    }

    private static String esc(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

import { ArrowDownToLine, BarChart3, Receipt, TrendingUp, Users } from 'lucide-react'
import { useState } from 'react'
import { useClientesRanking, useFaturamento, useServicosRanking } from '../../api/hooks'
import { ApiError } from '../../api/http'
import { baixarExportacao } from '../../api/relatorios'
import { Button } from '../../components/ui/Button'
import { PageHeader } from '../../components/ui/PageHeader'
import {
  EmptyState,
  ErrorState,
  FeedbackMessage,
  LoadingState,
} from '../../components/ui/States'
import { formatCurrency } from '../../lib/format'

function hoje(): string {
  return new Date().toISOString().slice(0, 10)
}

function inicioDoMes(): string {
  const data = new Date()
  return new Date(data.getFullYear(), data.getMonth(), 1).toISOString().slice(0, 10)
}

export function RelatoriosPage() {
  const [inicio, setInicio] = useState(inicioDoMes)
  const [fim, setFim] = useState(hoje)
  const [exportando, setExportando] = useState(false)
  const [erroExportacao, setErroExportacao] = useState<string | null>(null)

  const faturamento = useFaturamento(inicio, fim)
  const servicos = useServicosRanking(inicio, fim)
  const clientes = useClientesRanking(inicio, fim)

  const intervaloInvalido = Boolean(inicio && fim && inicio > fim)

  async function handleExportar() {
    setErroExportacao(null)
    setExportando(true)
    try {
      await baixarExportacao(inicio, fim)
    } catch (error) {
      setErroExportacao(
        error instanceof ApiError ? error.message : 'Não foi possível exportar o CSV.',
      )
    } finally {
      setExportando(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Relatórios"
        description="OS encerradas por data de fechamento, sempre com o valor congelado no snapshot."
        actions={
          <Button
            variant="secondary"
            loading={exportando}
            disabled={intervaloInvalido}
            onClick={() => void handleExportar()}
          >
            <ArrowDownToLine aria-hidden="true" className="size-4" />
            Exportar CSV
          </Button>
        }
      />

      {erroExportacao && (
        <FeedbackMessage tone="error" className="mt-4">
          {erroExportacao}
        </FeedbackMessage>
      )}

      <div className="mt-6 flex flex-wrap items-end gap-3 rounded-2xl border border-border bg-surface p-4">
        <label className="text-sm font-medium">
          <span className="mb-1.5 block text-ink-muted">Início</span>
          <input
            type="date"
            value={inicio}
            max={fim || undefined}
            onChange={(event) => setInicio(event.target.value)}
            className="h-10 rounded-md border border-border bg-surface px-3 text-sm"
          />
        </label>
        <label className="text-sm font-medium">
          <span className="mb-1.5 block text-ink-muted">Fim</span>
          <input
            type="date"
            value={fim}
            min={inicio || undefined}
            onChange={(event) => setFim(event.target.value)}
            className="h-10 rounded-md border border-border bg-surface px-3 text-sm"
          />
        </label>
        <p className="ml-auto text-xs text-ink-muted">
          Período inclusivo. Padrão: mês atual.
        </p>
      </div>

      {intervaloInvalido && (
        <p role="alert" className="mt-3 text-sm font-medium text-danger">
          A data de início não pode ser posterior à data de fim.
        </p>
      )}

      {!intervaloInvalido && faturamento.isLoading && (
        <div className="mt-6">
          <LoadingState label="Calculando faturamento…" />
        </div>
      )}

      {!intervaloInvalido && faturamento.isError && (
        <div className="mt-6">
          <ErrorState
            title="Não foi possível carregar o faturamento"
            message="Confira o período e tente novamente."
            onRetry={() => void faturamento.refetch()}
          />
        </div>
      )}

      {!intervaloInvalido && faturamento.data && (
        <>
          <section aria-label="Indicadores do período" className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <article className="rounded-2xl bg-primary p-5 text-on-primary">
              <div className="flex items-center justify-between">
                <p className="text-xs font-semibold tracking-wide uppercase opacity-80">Faturamento</p>
                <TrendingUp aria-hidden="true" className="size-4 opacity-80" />
              </div>
              <p className="mt-3 text-3xl font-semibold tracking-tight">
                {formatCurrency(faturamento.data.totalFinal)}
              </p>
              <p className="mt-1 text-xs opacity-80">
                {faturamento.data.quantidadeOs} OS fechada(s) · ticket {formatCurrency(faturamento.data.ticketMedio)}
              </p>
            </article>
            <article className="rounded-2xl border border-border bg-surface p-5">
              <p className="text-xs font-semibold tracking-wide text-ink-muted uppercase">Peças</p>
              <p className="mt-3 text-2xl font-semibold tracking-tight">
                {formatCurrency(faturamento.data.totalPecas)}
              </p>
              <p className="mt-1 text-xs text-ink-muted">Desconto aplicado: {formatCurrency(faturamento.data.descontoAplicado)}</p>
            </article>
            <article className="rounded-2xl border border-border bg-surface p-5">
              <p className="text-xs font-semibold tracking-wide text-ink-muted uppercase">Mão de obra</p>
              <p className="mt-3 text-2xl font-semibold tracking-tight">
                {formatCurrency(faturamento.data.totalMaoDeObra)}
              </p>
              <p className="mt-1 text-xs text-ink-muted">Com fator de risco por cilindrada</p>
            </article>
            <article className="rounded-2xl border border-border bg-surface p-5">
              <p className="text-xs font-semibold tracking-wide text-ink-muted uppercase">Ticket médio</p>
              <p className="mt-3 text-2xl font-semibold tracking-tight">
                {formatCurrency(faturamento.data.ticketMedio)}
              </p>
              <p className="mt-1 text-xs text-ink-muted">Total final ÷ OS fechadas</p>
            </article>
          </section>

          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            <section aria-labelledby="ranking-servicos" className="rounded-2xl border border-border bg-surface p-5">
              <div className="flex items-center gap-2">
                <BarChart3 aria-hidden="true" className="size-4 text-primary" />
                <h2 id="ranking-servicos" className="font-semibold">Serviços mais realizados</h2>
              </div>
              <div className="mt-4">
                {servicos.isLoading && <LoadingState label="Carregando serviços…" />}
                {servicos.isError && <ErrorState title="Ranking indisponível" message="Tente novamente." onRetry={() => void servicos.refetch()} />}
                {servicos.data && servicos.data.length === 0 && (
                  <EmptyState title="Nenhum serviço no período" description="Feche uma OS para começar o ranking." />
                )}
                {servicos.data && servicos.data.length > 0 && (
                  <ol className="divide-y divide-border/60">
                    {servicos.data.map((item) => (
                      <li key={`${item.tipo}-${item.descricao}`} className="flex items-start gap-3 py-3">
                        <span className="grid size-8 shrink-0 place-items-center rounded-full bg-primary-surface text-xs font-semibold text-primary">
                          {item.quantidade}×
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium">{item.descricao}</p>
                          <p className="text-xs text-ink-muted">
                            {item.tipo === 'REVISAO' ? 'Revisão' : 'Troca de peça'} · base {formatCurrency(item.totalBaseMaoDeObra)}
                          </p>
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </div>
            </section>

            <section aria-labelledby="ranking-clientes" className="rounded-2xl border border-border bg-surface p-5">
              <div className="flex items-center gap-2">
                <Users aria-hidden="true" className="size-4 text-primary" />
                <h2 id="ranking-clientes" className="font-semibold">Clientes com mais atendimentos</h2>
              </div>
              <div className="mt-4">
                {clientes.isLoading && <LoadingState label="Carregando clientes…" />}
                {clientes.isError && <ErrorState title="Ranking indisponível" message="Tente novamente." onRetry={() => void clientes.refetch()} />}
                {clientes.data && clientes.data.length === 0 && (
                  <EmptyState title="Nenhum cliente no período" description="Feche uma OS para começar o ranking." />
                )}
                {clientes.data && clientes.data.length > 0 && (
                  <ol className="divide-y divide-border/60">
                    {clientes.data.map((item) => (
                      <li key={item.clienteId} className="flex items-start gap-3 py-3">
                        <span className="grid size-8 shrink-0 place-items-center rounded-full bg-surface-muted text-xs font-semibold">
                          {item.quantidadeOs}×
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium">{item.clienteNome}</p>
                          <p className="text-xs text-ink-muted">
                            {formatCurrency(item.totalFinal)} em OS fechadas
                          </p>
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </div>
            </section>
          </div>

          <p className="mt-4 flex items-center gap-2 text-xs text-ink-muted">
            <Receipt aria-hidden="true" className="size-3.5" />
            Os valores vêm do snapshot gravado no fechamento de cada OS.
          </p>
        </>
      )}
    </div>
  )
}

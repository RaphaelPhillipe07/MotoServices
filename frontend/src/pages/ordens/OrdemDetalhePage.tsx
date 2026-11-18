import {
  ArrowLeft,
  CheckCircle2,
  ClipboardList,
  Clock,
  FileText,
  Play,
  Plus,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  useAlterarStatus,
  useCancelarOrdem,
  useConfirmarOrdem,
  useFecharOrdem,
  useHistorico,
  useOrcamento,
  useOrdem,
  useRemoverServico,
} from '../../api/hooks'
import { ApiError } from '../../api/http'
import { abrirComprovante } from '../../api/ordens'
import type { FormaPagamento, StatusOS } from '../../api/types'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { PageHeader } from '../../components/ui/PageHeader'
import {
  EmptyState,
  ErrorState,
  FeedbackMessage,
  LoadingState,
} from '../../components/ui/States'
import { StatusBadge } from '../../components/ui/StatusBadge'
import { SelectField } from '../../components/ui/TextField'
import { ServicoFormDialog } from '../../features/ordens/ServicoFormDialog'
import { formatCurrency, formatDateTime } from '../../lib/format'
import { statusMeta } from '../../lib/status'

type Feedback = { tone: 'success' | 'error'; texto: string }

function mensagemDeErro(error: unknown, padrao: string): string {
  return error instanceof ApiError ? error.message : padrao
}

function podeAdicionarServico(status: StatusOS): boolean {
  return status === 'ABERTA' || status === 'EM_ANDAMENTO' || status === 'AGUARDANDO_CLIENTE'
}

function podeFechar(status: StatusOS): boolean {
  return status === 'EM_ANDAMENTO' || status === 'AGUARDANDO_CLIENTE' || status === 'CONCLUIDA'
}

function podeCancelar(status: StatusOS): boolean {
  return status === 'ABERTA' || status === 'EM_ANDAMENTO' || status === 'AGUARDANDO_CLIENTE'
}

const formasPagamento: { valor: FormaPagamento; rotulo: string }[] = [
  { valor: 'PIX', rotulo: 'PIX' },
  { valor: 'DINHEIRO', rotulo: 'Dinheiro' },
  { valor: 'CARTAO_CREDITO', rotulo: 'Cartão de crédito' },
  { valor: 'CARTAO_DEBITO', rotulo: 'Cartão de débito' },
  { valor: 'BOLETO', rotulo: 'Boleto' },
  { valor: 'OUTRO', rotulo: 'Outro' },
]

export function OrdemDetalhePage() {
  const { osId } = useParams()
  const id = Number(osId)
  const idValido = Number.isInteger(id) && id > 0

  const ordemQuery = useOrdem(idValido ? id : 0)
  const orcamentoQuery = useOrcamento(idValido ? id : 0)
  const historicoQuery = useHistorico(idValido ? id : 0, 0, 20)
  const confirmar = useConfirmarOrdem(id)
  const fechar = useFecharOrdem(id)
  const cancelar = useCancelarOrdem(id)
  const alterarStatus = useAlterarStatus(id)
  const removerServico = useRemoverServico(id)

  const [servicoAberto, setServicoAberto] = useState(false)
  const [fechamentoAberto, setFechamentoAberto] = useState(false)
  const [cancelamentoAberto, setCancelamentoAberto] = useState(false)
  const [servicoParaRemover, setServicoParaRemover] = useState<number | null>(null)
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento>('PIX')
  const [comprovanteAberto, setComprovanteAberto] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  if (!idValido) {
    return (
      <ErrorState title="OS inválida" message="O identificador informado na URL não é válido." />
    )
  }

  if (ordemQuery.isLoading) {
    return <LoadingState label="Carregando ordem de serviço…" />
  }

  if (ordemQuery.isError || !ordemQuery.data) {
    return (
      <ErrorState
        message={mensagemDeErro(ordemQuery.error, 'Erro inesperado ao carregar a OS.')}
        onRetry={() => void ordemQuery.refetch()}
      />
    )
  }

  const ordem = ordemQuery.data
  const orcamento = orcamentoQuery.data ?? (
    ordem.totalFinal === null
      ? null
      : {
          totalPecas: ordem.totalPecas ?? 0,
          totalMaoDeObra: ordem.totalMaoDeObra ?? 0,
          descontoAplicado: ordem.descontoAplicado ?? 0,
          totalFinal: ordem.totalFinal,
        }
  )

  async function handleConfirmar() {
    setFeedback(null)
    try {
      await confirmar.mutateAsync()
      setFeedback({ tone: 'success', texto: 'OS confirmada e estoque baixado.' })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível confirmar a OS.'),
      })
    }
  }

  async function handleFechar() {
    setFeedback(null)
    try {
      const resultado = await fechar.mutateAsync(formaPagamento)
      setFechamentoAberto(false)
      setFeedback({
        tone: 'success',
        texto: `OS fechada com ${formaPagamento}. Total final: ${formatCurrency(resultado.totalFinal)}.`,
      })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível fechar a OS.'),
      })
    }
  }

  async function handleCancelar() {
    setFeedback(null)
    try {
      await cancelar.mutateAsync()
      setCancelamentoAberto(false)
      setFeedback({ tone: 'success', texto: 'OS cancelada. Estoque devolvido quando aplicável.' })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível cancelar a OS.'),
      })
    }
  }

  async function handleAlterarStatus(status: StatusOS) {
    setFeedback(null)
    try {
      await alterarStatus.mutateAsync(status)
      setFeedback({
        tone: 'success',
        texto: `Status alterado para ${statusMeta[status].label}.`,
      })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível alterar o status.'),
      })
    }
  }

  async function handleRemoverServico() {
    if (servicoParaRemover === null) {
      return
    }
    setFeedback(null)
    try {
      await removerServico.mutateAsync(servicoParaRemover)
      setServicoParaRemover(null)
      setFeedback({ tone: 'success', texto: 'Serviço removido. Estoque ajustado.' })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível remover o serviço.'),
      })
    }
  }

  async function handleAbrirComprovante() {
    setFeedback(null)
    setComprovanteAberto(true)
    try {
      await abrirComprovante(ordem.id)
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível abrir o comprovante.'),
      })
    } finally {
      setComprovanteAberto(false)
    }
  }

  return (
    <div>
      <Link
        to={`/ordens-servico?clienteId=${ordem.clienteId}`}
        className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Ordens de serviço
      </Link>

      <div className="mt-3">
        <PageHeader
          title={`OS #${ordem.id}`}
          description={`${ordem.clienteNome} · ${ordem.motoMarca} ${ordem.motoModelo} (${ordem.motoCilindradas} cc)`}
          actions={
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={ordem.status} />
              {podeAdicionarServico(ordem.status) && (
                <Button variant="secondary" onClick={() => setServicoAberto(true)}>
                  <Plus aria-hidden="true" className="size-4" />
                  Adicionar serviço
                </Button>
              )}
              {ordem.status === 'ABERTA' && (
                <Button loading={confirmar.isPending} onClick={() => void handleConfirmar()}>
                  <CheckCircle2 aria-hidden="true" className="size-4" />
                  Confirmar OS
                </Button>
              )}
              {ordem.status === 'EM_ANDAMENTO' && (
                <Button
                  variant="secondary"
                  loading={alterarStatus.isPending}
                  onClick={() => void handleAlterarStatus('AGUARDANDO_CLIENTE')}
                >
                  <Clock aria-hidden="true" className="size-4" />
                  Aguardar cliente
                </Button>
              )}
              {ordem.status === 'AGUARDANDO_CLIENTE' && (
                <Button
                  variant="secondary"
                  loading={alterarStatus.isPending}
                  onClick={() => void handleAlterarStatus('EM_ANDAMENTO')}
                >
                  <Play aria-hidden="true" className="size-4" />
                  Retomar serviço
                </Button>
              )}
              {podeFechar(ordem.status) && (
                <Button variant="secondary" onClick={() => setFechamentoAberto(true)}>
                  Fechar OS
                </Button>
              )}
              {podeCancelar(ordem.status) && (
                <Button variant="danger" onClick={() => setCancelamentoAberto(true)}>
                  Cancelar OS
                </Button>
              )}
              {ordem.status === 'FECHADA' && (
                <Button
                  variant="primary"
                  loading={comprovanteAberto}
                  onClick={() => void handleAbrirComprovante()}
                >
                  <FileText aria-hidden="true" className="size-4" />
                  Comprovante
                </Button>
              )}
            </div>
          }
        />
      </div>

      {feedback && (
        <FeedbackMessage className="mt-4" tone={feedback.tone}>
          {feedback.texto}
        </FeedbackMessage>
      )}

      <dl className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Cliente</dt>
          <dd className="mt-1 font-medium">
            <Link to={`/clientes/${ordem.clienteId}`} className="text-primary hover:underline">
              {ordem.clienteNome}
            </Link>
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Moto</dt>
          <dd className="mt-1 font-medium">
            {ordem.motoMarca} {ordem.motoModelo}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Abertura</dt>
          <dd className="mt-1 font-medium">{formatDateTime(ordem.dataAbertura)}</dd>
        </div>
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Fechamento</dt>
          <dd className="mt-1 font-medium">{formatDateTime(ordem.dataFechamento)}</dd>
        </div>
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Pagamento</dt>
          <dd className="mt-1 font-medium">
            {ordem.formaPagamento ?? (ordem.status === 'FECHADA' ? 'Não informado' : '—')}
          </dd>
        </div>
      </dl>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <section aria-labelledby="servicos-os">
          <h2 id="servicos-os" className="text-lg font-semibold">
            Serviços
          </h2>

          <div className="mt-3">
            {ordem.servicos.length === 0 ? (
              <EmptyState
                icon={ClipboardList}
                title="Nenhum serviço adicionado"
                description="Adicione revisões ou trocas de peça para montar o orçamento."
                action={
                  podeAdicionarServico(ordem.status) ? (
                    <Button variant="secondary" onClick={() => setServicoAberto(true)}>
                      Adicionar serviço
                    </Button>
                  ) : undefined
                }
              />
            ) : (
              <div className="overflow-x-auto rounded-xl border border-border bg-surface">
                <table className="min-w-full text-left text-sm">
                  <caption className="sr-only">Serviços da ordem</caption>
                  <thead className="border-b border-border bg-surface-muted text-xs tracking-wide text-ink-muted uppercase">
                    <tr>
                      <th scope="col" className="px-4 py-3 font-medium">
                        Tipo
                      </th>
                      <th scope="col" className="px-4 py-3 font-medium">
                        Descrição
                      </th>
                      <th scope="col" className="px-4 py-3 font-medium">
                        Peça
                      </th>
                      <th scope="col" className="px-4 py-3 text-right font-medium">
                        Valor final
                      </th>
                      {podeAdicionarServico(ordem.status) && (
                        <th scope="col" className="px-4 py-3 text-right font-medium">
                          Ações
                        </th>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {ordem.servicos.map((servico) => (
                      <tr key={servico.id} className="border-b border-border last:border-b-0">
                        <td className="px-4 py-3">
                          {servico.tipo === 'REVISAO' ? (
                            <Badge variant="info">Revisão</Badge>
                          ) : (
                            <Badge variant="warning">Troca de peça</Badge>
                          )}
                        </td>
                        <th scope="row" className="px-4 py-3 font-medium">
                          {servico.descricao}
                        </th>
                        <td className="px-4 py-3">
                          {servico.tipo === 'TROCA_PECA'
                            ? servico.pecaFornecidaPelaLoja
                              ? `Loja · #${servico.pecaId}`
                              : 'Trazida pelo cliente'
                            : '—'}
                        </td>
                        <td className="tnum px-4 py-3 text-right">
                          {formatCurrency(servico.valorFinal)}
                        </td>
                        {podeAdicionarServico(ordem.status) && (
                          <td className="px-4 py-3 text-right">
                            <Button
                              variant="ghost"
                              size="icon"
                              aria-label={`Remover ${servico.descricao}`}
                              onClick={() => setServicoParaRemover(servico.id)}
                            >
                              <Trash2 aria-hidden="true" className="size-4" />
                            </Button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </section>

        <section aria-labelledby="orcamento-os">
          <h2 id="orcamento-os" className="text-lg font-semibold">
            Orçamento
          </h2>

          <div className="mt-3 rounded-xl border border-border bg-surface p-5">
            {orcamentoQuery.isLoading && !orcamento && <LoadingState label="Calculando…" />}

            {orcamentoQuery.isError && !orcamento && (
              <ErrorState
                message={mensagemDeErro(orcamentoQuery.error, 'Não foi possível calcular o orçamento.')}
                onRetry={() => void orcamentoQuery.refetch()}
              />
            )}

            {orcamento && (
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-ink-muted">Peças</dt>
                  <dd className="tnum">{formatCurrency(orcamento.totalPecas)}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-ink-muted">Mão de obra</dt>
                  <dd className="tnum">{formatCurrency(orcamento.totalMaoDeObra)}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-ink-muted">Desconto</dt>
                  <dd className="tnum">− {formatCurrency(orcamento.descontoAplicado)}</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-border pt-2 text-base font-semibold">
                  <dt>Total</dt>
                  <dd className="tnum">{formatCurrency(orcamento.totalFinal)}</dd>
                </div>
              </dl>
            )}
          </div>
        </section>
      </div>

      <section aria-labelledby="historico-os" className="mt-8">
        <h2 id="historico-os" className="text-lg font-semibold">
          Histórico de status
        </h2>

        <div className="mt-3">
          {historicoQuery.isLoading && <LoadingState label="Carregando histórico…" />}
          {historicoQuery.isError && (
            <ErrorState
              message={mensagemDeErro(historicoQuery.error, 'Não foi possível carregar o histórico.')}
              onRetry={() => void historicoQuery.refetch()}
            />
          )}
          {historicoQuery.data && historicoQuery.data.content.length === 0 && (
            <EmptyState title="Sem transições registradas" />
          )}
          {historicoQuery.data && historicoQuery.data.content.length > 0 && (
            <ol className="space-y-3">
              {historicoQuery.data.content.map((item) => (
                <li
                  key={item.id}
                  className="flex flex-wrap items-center gap-3 rounded-xl border border-border bg-surface p-4"
                >
                  <StatusBadge status={item.statusNovo} />
                  <p className="text-sm text-ink-muted">
                    {item.statusAnterior
                      ? `${statusMeta[item.statusAnterior].label} → ${statusMeta[item.statusNovo].label}`
                      : `OS aberta como ${statusMeta[item.statusNovo].label}`}
                  </p>
                  <time dateTime={item.dataHora} className="ml-auto text-xs text-ink-muted">
                    {formatDateTime(item.dataHora)}
                  </time>
                </li>
              ))}
            </ol>
          )}
        </div>
      </section>

      <ServicoFormDialog
        osId={ordem.id}
        open={servicoAberto}
        onOpenChange={setServicoAberto}
        onCreated={(mensagem) => setFeedback({ tone: 'success', texto: mensagem })}
      />

      <ConfirmDialog
        open={cancelamentoAberto}
        onOpenChange={setCancelamentoAberto}
        title="Cancelar OS"
        description="A OS será encerrada como CANCELADA. Se o estoque já foi baixado, as peças da loja serão devolvidas automaticamente."
        confirmLabel="Cancelar OS"
        destructive
        loading={cancelar.isPending}
        onConfirm={() => void handleCancelar()}
      />

      <ConfirmDialog
        open={servicoParaRemover !== null}
        onOpenChange={(aberto) => !aberto && setServicoParaRemover(null)}
        title="Remover serviço"
        description="O serviço sai do orçamento. Se a OS já foi confirmada, a peça da loja volta ao estoque."
        confirmLabel="Remover serviço"
        destructive
        loading={removerServico.isPending}
        onConfirm={() => void handleRemoverServico()}
      />

      <Dialog open={fechamentoAberto} onOpenChange={setFechamentoAberto}>
        <DialogContent
          title="Fechar OS"
          description="O orçamento será congelado, a revisão do cliente será agendada e a OS não poderá mais ser alterada."
        >
          <div className="rounded-xl bg-surface-muted px-4 py-3 text-sm">
            <div className="flex justify-between gap-4">
              <span className="text-ink-muted">Total a pagar</span>
              <span className="tnum font-semibold">
                {formatCurrency(orcamento?.totalFinal ?? ordem.totalFinal ?? 0)}
              </span>
            </div>
          </div>
          <div className="mt-4">
            <SelectField
              label="Forma de pagamento"
              value={formaPagamento}
              onChange={(event) => setFormaPagamento(event.target.value as FormaPagamento)}
            >
              {formasPagamento.map((forma) => (
                <option key={forma.valor} value={forma.valor}>
                  {forma.rotulo}
                </option>
              ))}
            </SelectField>
          </div>
          <div className="mt-6 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setFechamentoAberto(false)}>
              Voltar
            </Button>
            <Button loading={fechar.isPending} onClick={() => void handleFechar()}>
              Confirmar fechamento
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}

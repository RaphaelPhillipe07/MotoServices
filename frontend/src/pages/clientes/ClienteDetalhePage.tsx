import { ArrowLeft, Bike, Link2, Unlink } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  useCliente,
  useDesvincularMoto,
  useMotos,
  useMotosDoCliente,
  useVincularMoto,
} from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Moto } from '../../api/types'
import { Button, buttonVariants } from '../../components/ui/Button'
import { PageHeader } from '../../components/ui/PageHeader'
import {
  EmptyState,
  ErrorState,
  FeedbackMessage,
  LoadingState,
} from '../../components/ui/States'
import { SelectField } from '../../components/ui/TextField'
import { cn } from '../../lib/cn'
import { formatDate, formatDocumento } from '../../lib/format'

type Feedback = { tone: 'success' | 'error'; texto: string }

function mensagemDeErro(error: unknown, padrao: string): string {
  return error instanceof ApiError ? error.message : padrao
}

export function ClienteDetalhePage() {
  const { clienteId } = useParams()
  const id = Number(clienteId)
  const idValido = Number.isInteger(id) && id > 0

  const clienteQuery = useCliente(idValido ? id : 0)
  const motosQuery = useMotosDoCliente(id, 0, 100)
  const catalogoQuery = useMotos(0, 100)
  const vincular = useVincularMoto()
  const desvincular = useDesvincularMoto()

  const [motoSelecionada, setMotoSelecionada] = useState('')
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  if (!idValido) {
    return (
      <ErrorState
        title="Cliente inválido"
        message="O identificador informado na URL não é válido."
      />
    )
  }

  if (clienteQuery.isLoading) {
    return <LoadingState label="Carregando cliente…" />
  }

  if (clienteQuery.isError || !clienteQuery.data) {
    return (
      <ErrorState
        message={mensagemDeErro(clienteQuery.error, 'Erro inesperado ao carregar o cliente.')}
        onRetry={() => void clienteQuery.refetch()}
      />
    )
  }

  const cliente = clienteQuery.data
  const motos = motosQuery.data?.content ?? []
  const idsVinculados = new Set(motos.map((moto) => moto.id))
  const disponiveis = (catalogoQuery.data?.content ?? []).filter(
    (moto) => !idsVinculados.has(moto.id),
  )

  async function handleVincular(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!motoSelecionada) return
    setFeedback(null)
    try {
      await vincular.mutateAsync({ clienteId: id, motoId: Number(motoSelecionada) })
      setMotoSelecionada('')
      setFeedback({ tone: 'success', texto: 'Moto vinculada ao cliente.' })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível vincular a moto.'),
      })
    }
  }

  async function handleDesvincular(moto: Moto) {
    setFeedback(null)
    try {
      await desvincular.mutateAsync({ clienteId: id, motoId: moto.id })
      setFeedback({ tone: 'success', texto: `${moto.marca} ${moto.modelo} desvinculada.` })
    } catch (error) {
      setFeedback({
        tone: 'error',
        texto: mensagemDeErro(error, 'Não foi possível desvincular a moto.'),
      })
    }
  }

  return (
    <div>
      <Link
        to="/clientes"
        className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Clientes
      </Link>

      <div className="mt-3">
        <PageHeader
          title={cliente.nome}
          description={`${formatDocumento(cliente.cpfCnpj)} · ${cliente.contato}`}
          actions={
            <Link
              to={`/ordens-servico?clienteId=${cliente.id}`}
              className={cn(buttonVariants({ variant: 'secondary' }))}
            >
              Ver ordens de serviço
            </Link>
          }
        />
      </div>

      <dl className="mt-6 grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">CPF/CNPJ</dt>
          <dd className="tnum mt-1 font-medium">{formatDocumento(cliente.cpfCnpj)}</dd>
        </div>
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Contato</dt>
          <dd className="mt-1 font-medium">{cliente.contato}</dd>
        </div>
        <div className="rounded-xl border border-border bg-surface p-4">
          <dt className="text-xs tracking-wide text-ink-muted uppercase">Próxima revisão</dt>
          <dd className="mt-1 font-medium">{formatDate(cliente.proximaRevisao)}</dd>
        </div>
      </dl>

      <section aria-labelledby="motos-cliente" className="mt-8">
        <h2 id="motos-cliente" className="text-lg font-semibold">
          Motos do cliente
        </h2>
        <p className="mt-1 text-sm text-ink-muted">
          O vínculo também é criado automaticamente na primeira OS desta moto.
        </p>

        {feedback && (
          <FeedbackMessage className="mt-3" tone={feedback.tone}>
            {feedback.texto}
          </FeedbackMessage>
        )}

        <div className="mt-3">
          {motosQuery.isLoading && <LoadingState label="Carregando motos…" />}
          {motosQuery.isError && (
            <ErrorState
              message={mensagemDeErro(motosQuery.error, 'Erro ao carregar as motos do cliente.')}
              onRetry={() => void motosQuery.refetch()}
            />
          )}
          {motosQuery.data && motos.length === 0 && (
            <EmptyState
              icon={Bike}
              title="Nenhuma moto vinculada"
              description="Selecione uma moto do catálogo para criar o vínculo."
            />
          )}
          {motos.length > 0 && (
            <div className="overflow-x-auto rounded-xl border border-border bg-surface">
              <table className="min-w-full text-left text-sm">
                <caption className="sr-only">Motos vinculadas ao cliente</caption>
                <thead className="border-b border-border bg-surface-muted text-xs tracking-wide text-ink-muted uppercase">
                  <tr>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Marca
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Modelo
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Cilindradas
                    </th>
                    <th scope="col" className="px-4 py-3 text-right font-medium">
                      Ações
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {motos.map((moto) => (
                    <tr key={moto.id} className="border-b border-border last:border-b-0">
                      <th scope="row" className="px-4 py-3 font-medium">
                        {moto.marca}
                      </th>
                      <td className="px-4 py-3">{moto.modelo}</td>
                      <td className="tnum px-4 py-3">{moto.cilindradas} cc</td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          loading={desvincular.isPending}
                          onClick={() => void handleDesvincular(moto)}
                        >
                          <Unlink aria-hidden="true" className="size-4" />
                          Desvincular
                          <span className="sr-only">
                            {' '}
                            {moto.marca} {moto.modelo}
                          </span>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <form
          onSubmit={handleVincular}
          className="mt-4 flex flex-col gap-3 rounded-xl border border-border bg-surface p-4 sm:flex-row sm:items-start"
        >
          <div className="flex-1">
            <SelectField
              label="Vincular moto do catálogo"
              value={motoSelecionada}
              onChange={(event) => setMotoSelecionada(event.target.value)}
              hint={
                disponiveis.length === 0
                  ? 'Todas as motos do catálogo já estão vinculadas.'
                  : 'Selecione uma moto ainda não vinculada.'
              }
              disabled={disponiveis.length === 0}
            >
              <option value="">Selecione…</option>
              {disponiveis.map((moto) => (
                <option key={moto.id} value={moto.id}>
                  {moto.marca} {moto.modelo} ({moto.cilindradas} cc)
                </option>
              ))}
            </SelectField>
          </div>
          <Button
            type="submit"
            className="sm:mt-6"
            loading={vincular.isPending}
            disabled={!motoSelecionada}
          >
            <Link2 aria-hidden="true" className="size-4" />
            Vincular
          </Button>
        </form>
      </section>
    </div>
  )
}

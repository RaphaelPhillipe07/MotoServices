import { ClipboardList, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useCliente, useOrdens } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { StatusOS } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { PageHeader } from '../../components/ui/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../components/ui/States'
import { SelectField } from '../../components/ui/TextField'
import { StatusBadge } from '../../components/ui/StatusBadge'
import { OrdemFormDialog } from '../../features/ordens/OrdemFormDialog'
import { formatCurrency, formatDate } from '../../lib/format'
import { statusMeta, statusOrder } from '../../lib/status'

function mensagemDeErro(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : 'Erro inesperado ao consultar as ordens de serviço.'
}

export function OrdensPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const clienteId = Number(searchParams.get('clienteId')) || undefined
  const statusParam = searchParams.get('status')
  const statusInicial = statusOrder.includes(statusParam as StatusOS)
    ? (statusParam as StatusOS)
    : ''
  const [status, setStatus] = useState<StatusOS | ''>(statusInicial)
  const [page, setPage] = useState(0)
  const [formAberto, setFormAberto] = useState(searchParams.get('nova') === '1')

  const consulta = useOrdens(clienteId, status || undefined, page, 20)
  const cliente = useCliente(clienteId ?? 0)
  const ordens = consulta.data?.content ?? []
  const temFiltro = Boolean(clienteId || status)

  function atualizarStatus(novo: StatusOS | '') {
    setStatus(novo)
    setPage(0)
    const proximos = new URLSearchParams(searchParams)
    if (novo) {
      proximos.set('status', novo)
    } else {
      proximos.delete('status')
    }
    setSearchParams(proximos, { replace: true })
  }

  function limparFiltros() {
    setStatus('')
    setPage(0)
    setSearchParams({}, { replace: true })
  }

  function alterarFormulario(aberto: boolean) {
    setFormAberto(aberto)
    if (!aberto && searchParams.has('nova')) {
      const proximos = new URLSearchParams(searchParams)
      proximos.delete('nova')
      setSearchParams(proximos, { replace: true })
    }
  }

  return (
    <div>
      <PageHeader
        title="Ordens de serviço"
        description="Acompanhe o ciclo de vida das OS: abertura, execução e fechamento com orçamento."
        actions={
          <Button onClick={() => setFormAberto(true)}>
            <Plus aria-hidden="true" className="size-4" />
            Nova OS
          </Button>
        }
      />

      <div className="mt-6 flex flex-wrap items-end gap-3 rounded-xl border border-border bg-surface p-4">
        <div className="w-full sm:w-56">
          <SelectField
            label="Status"
            value={status}
            onChange={(event) => {
              atualizarStatus(event.target.value as StatusOS | '')
            }}
          >
            <option value="">Todos os status</option>
            {statusOrder.map((valor) => (
              <option key={valor} value={valor}>
                {statusMeta[valor].label}
              </option>
            ))}
          </SelectField>
        </div>

        {clienteId && (
          <p className="text-sm text-ink-muted">
            Cliente:{' '}
            <span className="font-medium text-ink">
              {cliente.data?.nome ?? `#${clienteId}`}
            </span>
          </p>
        )}

        {temFiltro && (
          <Button variant="ghost" size="sm" onClick={limparFiltros}>
            Limpar filtros
          </Button>
        )}
      </div>

      <section aria-labelledby="lista-ordens" className="mt-6">
        <h2 id="lista-ordens" className="sr-only">
          Ordens de serviço
        </h2>

        {consulta.isLoading && <LoadingState label="Carregando ordens de serviço…" />}

        {consulta.isError && (
          <ErrorState
            message={mensagemDeErro(consulta.error)}
            onRetry={() => void consulta.refetch()}
          />
        )}

        {consulta.data && ordens.length === 0 && (
          <EmptyState
            icon={ClipboardList}
            title={temFiltro ? 'Nenhuma OS com esses filtros' : 'Nenhuma OS aberta'}
            description={
              temFiltro
                ? 'Ajuste o status ou remova o filtro de cliente para ver mais resultados.'
                : 'Abra a primeira ordem de serviço para começar o atendimento.'
            }
            action={
              temFiltro ? (
                <Button variant="secondary" onClick={limparFiltros}>
                  Limpar filtros
                </Button>
              ) : (
                <Button onClick={() => setFormAberto(true)}>Abrir OS</Button>
              )
            }
          />
        )}

        {ordens.length > 0 && (
          <>
            {consulta.isFetching && (
              <p role="status" className="mb-2 text-sm text-ink-muted">
                Atualizando ordens…
              </p>
            )}
            <div className="overflow-x-auto rounded-xl border border-border bg-surface">
              <table className="min-w-full text-left text-sm">
                <caption className="sr-only">Ordens de serviço encontradas</caption>
                <thead className="border-b border-border bg-surface-muted text-xs tracking-wide text-ink-muted uppercase">
                  <tr>
                    <th scope="col" className="px-4 py-3 font-medium">
                      OS
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Cliente
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Moto
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Status
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Abertura
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Total
                    </th>
                    <th scope="col" className="px-4 py-3 text-right font-medium">
                      Ações
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {ordens.map((os) => (
                    <tr key={os.id} className="border-b border-border last:border-b-0">
                      <th scope="row" className="tnum px-4 py-3 font-medium">
                        #{os.id}
                      </th>
                      <td className="px-4 py-3">
                        <Link
                          to={`/clientes/${os.clienteId}`}
                          className="font-medium text-primary hover:underline"
                        >
                          {os.clienteNome}
                        </Link>
                      </td>
                      <td className="px-4 py-3">
                        {os.motoMarca} {os.motoModelo}{' '}
                        <span className="text-ink-muted">({os.motoCilindradas} cc)</span>
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={os.status} />
                      </td>
                      <td className="px-4 py-3">{formatDate(os.dataAbertura)}</td>
                      <td className="tnum px-4 py-3">{formatCurrency(os.totalFinal)}</td>
                      <td className="px-4 py-3 text-right">
                        <Link
                          to={`/ordens-servico/${os.id}`}
                          className="inline-flex rounded-md px-2 py-1 text-sm font-medium text-primary hover:bg-primary-surface"
                        >
                          Abrir
                          <span className="sr-only"> OS {os.id}</span>
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={consulta.data?.page ?? 0}
              totalPages={consulta.data?.totalPages ?? 1}
              totalElements={consulta.data?.totalElements ?? 0}
              itemLabel="ordens"
              onPageChange={setPage}
            />
          </>
        )}
      </section>

      <OrdemFormDialog open={formAberto} onOpenChange={alterarFormulario} />
    </div>
  )
}

import { Bike, Plus } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMotosFiltradas } from '../../api/hooks'
import { ApiError } from '../../api/http'
import { Button } from '../../components/ui/Button'
import { PageHeader } from '../../components/ui/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import {
  EmptyState,
  ErrorState,
  FeedbackMessage,
  LoadingState,
} from '../../components/ui/States'
import { TextField } from '../../components/ui/TextField'
import { MotoFormDialog } from '../../features/motos/MotoFormDialog'

function mensagemDeErro(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Erro inesperado ao consultar o catálogo.'
}

export function MotosPage() {
  const [minimo, setMinimo] = useState('1')
  const [maximo, setMaximo] = useState('2000')
  const [filtro, setFiltro] = useState({ minimo: 1, maximo: 2000 })
  const [page, setPage] = useState(0)
  const [erroFiltro, setErroFiltro] = useState<string | null>(null)
  const [formAberto, setFormAberto] = useState(false)
  const [sucesso, setSucesso] = useState<string | null>(null)

  const consulta = useMotosFiltradas(filtro.minimo, filtro.maximo, page, 10)
  const motos = consulta.data?.content ?? []

  function filtrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const min = Number(minimo)
    const max = Number(maximo)
    if (!Number.isInteger(min) || min <= 0) {
      setErroFiltro('Informe uma cilindrada mínima positiva.')
      return
    }
    if (!Number.isInteger(max) || max < min) {
      setErroFiltro('A cilindrada máxima deve ser maior ou igual à mínima.')
      return
    }
    setErroFiltro(null)
    setSucesso(null)
    setFiltro({ minimo: min, maximo: max })
    setPage(0)
  }

  return (
    <div>
      <PageHeader
        title="Motos"
        description="Catálogo de marcas e modelos usado na abertura das ordens de serviço."
        actions={
          <Button onClick={() => setFormAberto(true)}>
            <Plus aria-hidden="true" className="size-4" />
            Nova moto
          </Button>
        }
      />

      {sucesso && (
        <FeedbackMessage className="mt-4" tone="success">
          {sucesso}
        </FeedbackMessage>
      )}

      <form
        onSubmit={filtrar}
        noValidate
        aria-label="Filtrar motos por cilindradas"
        className="mt-6 flex flex-col gap-3 rounded-xl border border-border bg-surface p-4 sm:flex-row sm:items-start"
      >
        <div className="sm:w-44">
          <TextField
            label="Cilindradas mínimas"
            type="number"
            min={1}
            value={minimo}
            error={erroFiltro ?? undefined}
            onChange={(event) => {
              setMinimo(event.target.value)
              if (erroFiltro) setErroFiltro(null)
            }}
          />
        </div>
        <div className="sm:w-44">
          <TextField
            label="Cilindradas máximas"
            type="number"
            min={1}
            value={maximo}
            onChange={(event) => setMaximo(event.target.value)}
          />
        </div>
        <Button type="submit" className="sm:mt-6">
          Filtrar
        </Button>
      </form>

      <section aria-labelledby="resultados-motos" className="mt-6">
        <h2 id="resultados-motos" className="sr-only">
          Motos do catálogo
        </h2>

        {consulta.isLoading && <LoadingState label="Carregando catálogo…" />}

        {consulta.isError && (
          <ErrorState
            message={mensagemDeErro(consulta.error)}
            onRetry={() => void consulta.refetch()}
          />
        )}

        {consulta.data && motos.length === 0 && (
          <EmptyState
            icon={Bike}
            title="Nenhuma moto nessa faixa"
            description="Ajuste as cilindradas ou cadastre uma nova moto no catálogo."
            action={
              <Button variant="secondary" onClick={() => setFormAberto(true)}>
                Cadastrar moto
              </Button>
            }
          />
        )}

        {motos.length > 0 && (
          <>
            {consulta.isFetching && (
              <p role="status" className="mb-2 text-sm text-ink-muted">
                Atualizando catálogo…
              </p>
            )}
            <div className="overflow-x-auto rounded-xl border border-border bg-surface">
              <table className="min-w-full text-left text-sm">
                <caption className="sr-only">Motos encontradas no catálogo</caption>
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
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={consulta.data?.page ?? 0}
              totalPages={consulta.data?.totalPages ?? 1}
              totalElements={consulta.data?.totalElements ?? 0}
              itemLabel="motos"
              onPageChange={setPage}
            />
          </>
        )}
      </section>

      <MotoFormDialog
        open={formAberto}
        onOpenChange={setFormAberto}
        onCreated={(moto) => {
          setSucesso(`Moto ${moto.marca} ${moto.modelo} cadastrada.`)
        }}
      />
    </div>
  )
}

import { Plus, Search, Users } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useBuscaClientes } from '../../api/hooks'
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
import { ClienteFormDialog } from '../../features/clientes/ClienteFormDialog'
import { formatDate, formatDocumento } from '../../lib/format'

function mensagemDeErro(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : 'Erro inesperado ao consultar clientes.'
}

export function ClientesPage() {
  const [termo, setTermo] = useState('')
  const [busca, setBusca] = useState('')
  const [page, setPage] = useState(0)
  const [erroBusca, setErroBusca] = useState<string | null>(null)
  const [formAberto, setFormAberto] = useState(false)
  const [sucesso, setSucesso] = useState<string | null>(null)

  const consulta = useBuscaClientes(busca, page, 10)
  const clientes = consulta.data?.content ?? []

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const valor = termo.trim()
    if (!valor) {
      setErroBusca('Informe um nome, CPF ou CNPJ para buscar.')
      return
    }
    setErroBusca(null)
    setSucesso(null)
    setBusca(valor)
    setPage(0)
  }

  return (
    <div>
      <PageHeader
        title="Clientes"
        description="Busque por nome ou documento, cadastre novos clientes e gerencie as motos vinculadas."
        actions={
          <Button onClick={() => setFormAberto(true)}>
            <Plus aria-hidden="true" className="size-4" />
            Novo cliente
          </Button>
        }
      />

      {sucesso && (
        <FeedbackMessage className="mt-4" tone="success">
          {sucesso}
        </FeedbackMessage>
      )}

      <form
        role="search"
        onSubmit={buscar}
        noValidate
        className="mt-6 flex flex-col gap-3 rounded-xl border border-border bg-surface p-4 sm:flex-row sm:items-start"
      >
        <div className="flex-1">
          <TextField
            label="Buscar cliente"
            placeholder="Nome, CPF ou CNPJ"
            hint="A busca por nome é paginada; documento retorna no máximo um cliente."
            value={termo}
            error={erroBusca ?? undefined}
            onChange={(event) => {
              setTermo(event.target.value)
              if (erroBusca) setErroBusca(null)
            }}
          />
        </div>
        <Button type="submit" className="sm:mt-6">
          <Search aria-hidden="true" className="size-4" />
          Buscar
        </Button>
      </form>

      <section aria-labelledby="resultados-clientes" className="mt-6">
        <h2 id="resultados-clientes" className="sr-only">
          Resultados da busca
        </h2>

        {!busca && (
          <EmptyState
            icon={Users}
            title="Busque um cliente"
            description="Digite um nome ou CPF/CNPJ para ver os clientes cadastrados."
          />
        )}

        {busca && consulta.isLoading && <LoadingState label="Buscando clientes…" />}

        {busca && consulta.isError && (
          <ErrorState
            message={mensagemDeErro(consulta.error)}
            onRetry={() => void consulta.refetch()}
          />
        )}

        {busca && consulta.data && !consulta.isLoading && clientes.length === 0 && (
          <EmptyState
            icon={Users}
            title="Nenhum cliente encontrado"
            description={`Não encontramos resultados para “${busca}”. Revise o termo ou cadastre um novo cliente.`}
            action={
              <Button variant="secondary" onClick={() => setFormAberto(true)}>
                Cadastrar cliente
              </Button>
            }
          />
        )}

        {clientes.length > 0 && (
          <>
            {consulta.isFetching && (
              <p role="status" className="mb-2 text-sm text-ink-muted">
                Atualizando resultados…
              </p>
            )}
            <div className="overflow-x-auto rounded-xl border border-border bg-surface">
              <table className="min-w-full text-left text-sm">
                <caption className="sr-only">Clientes encontrados</caption>
                <thead className="border-b border-border bg-surface-muted text-xs tracking-wide text-ink-muted uppercase">
                  <tr>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Nome
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      CPF/CNPJ
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Contato
                    </th>
                    <th scope="col" className="px-4 py-3 font-medium">
                      Próxima revisão
                    </th>
                    <th scope="col" className="px-4 py-3 text-right font-medium">
                      Ações
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {clientes.map((cliente) => (
                    <tr key={cliente.id} className="border-b border-border last:border-b-0">
                      <th scope="row" className="px-4 py-3 font-medium">
                        {cliente.nome}
                      </th>
                      <td className="tnum px-4 py-3">{formatDocumento(cliente.cpfCnpj)}</td>
                      <td className="px-4 py-3">{cliente.contato}</td>
                      <td className="px-4 py-3">{formatDate(cliente.proximaRevisao)}</td>
                      <td className="px-4 py-3 text-right">
                        <Link
                          to={`/clientes/${cliente.id}`}
                          className="inline-flex rounded-md px-2 py-1 text-sm font-medium text-primary hover:bg-primary-surface"
                        >
                          Detalhes
                          <span className="sr-only"> de {cliente.nome}</span>
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
              itemLabel="clientes"
              onPageChange={(novaPagina) => {
                setPage(novaPagina)
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            />
          </>
        )}
      </section>

      <ClienteFormDialog
        open={formAberto}
        onOpenChange={setFormAberto}
        onCreated={(cliente) => {
          setSucesso(`Cliente ${cliente.nome} cadastrado.`)
          setTermo(cliente.nome)
          setBusca(cliente.nome)
          setPage(0)
        }}
      />
    </div>
  )
}

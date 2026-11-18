import type { UseQueryResult } from '@tanstack/react-query'
import { Package, PackagePlus, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { useBaixoEstoque, usePecas } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Page, Peca } from '../../api/types'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { PageHeader } from '../../components/ui/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import {
  EmptyState,
  ErrorState,
  FeedbackMessage,
  LoadingState,
} from '../../components/ui/States'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/Tabs'
import { TextField } from '../../components/ui/TextField'
import { EntradaEstoqueDialog } from '../../features/pecas/EntradaEstoqueDialog'
import { PecaFormDialog } from '../../features/pecas/PecaFormDialog'
import { formatCurrency } from '../../lib/format'

function mensagemDeErro(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Erro inesperado ao consultar o estoque.'
}

type PecasListaProps = {
  consulta: UseQueryResult<Page<Peca>>
  vazioTitulo: string
  vazioDescricao: string
  onEntrada: (peca: Peca) => void
  onCadastrar: () => void
  onPageChange: (page: number) => void
}

function PecasLista({
  consulta,
  vazioTitulo,
  vazioDescricao,
  onEntrada,
  onCadastrar,
  onPageChange,
}: PecasListaProps) {
  const pecas = consulta.data?.content ?? []

  if (consulta.isLoading) {
    return <LoadingState label="Carregando peças…" />
  }

  if (consulta.isError) {
    return (
      <ErrorState message={mensagemDeErro(consulta.error)} onRetry={() => void consulta.refetch()} />
    )
  }

  if (consulta.data && pecas.length === 0) {
    return (
      <EmptyState
        icon={Package}
        title={vazioTitulo}
        description={vazioDescricao}
        action={
          <Button variant="secondary" onClick={onCadastrar}>
            Cadastrar peça
          </Button>
        }
      />
    )
  }

  if (pecas.length === 0) return null

  return (
    <>
      {consulta.isFetching && (
        <p role="status" className="mb-2 text-sm text-ink-muted">
          Atualizando estoque…
        </p>
      )}
      <div className="overflow-x-auto rounded-xl border border-border bg-surface">
        <table className="min-w-full text-left text-sm">
          <caption className="sr-only">Peças em estoque</caption>
          <thead className="border-b border-border bg-surface-muted text-xs tracking-wide text-ink-muted uppercase">
            <tr>
              <th scope="col" className="px-4 py-3 font-medium">
                Código
              </th>
              <th scope="col" className="px-4 py-3 font-medium">
                Descrição
              </th>
              <th scope="col" className="px-4 py-3 font-medium">
                Valor
              </th>
              <th scope="col" className="px-4 py-3 font-medium">
                Estoque
              </th>
              <th scope="col" className="px-4 py-3 font-medium">
                Situação
              </th>
              <th scope="col" className="px-4 py-3 text-right font-medium">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            {pecas.map((peca) => {
              const baixo = peca.quantidadeEstoque < peca.estoqueMinimo
              return (
                <tr key={peca.id} className="border-b border-border last:border-b-0">
                  <th scope="row" className="tnum px-4 py-3 font-medium">
                    {peca.codigo}
                  </th>
                  <td className="px-4 py-3">{peca.descricao}</td>
                  <td className="tnum px-4 py-3">{formatCurrency(peca.valor)}</td>
                  <td className="tnum px-4 py-3">
                    {peca.quantidadeEstoque} / {peca.estoqueMinimo}
                  </td>
                  <td className="px-4 py-3">
                    {baixo ? (
                      <Badge variant="danger">Abaixo do mínimo</Badge>
                    ) : (
                      <Badge variant="success">OK</Badge>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button variant="secondary" size="sm" onClick={() => onEntrada(peca)}>
                      Entrada
                      <span className="sr-only"> para {peca.codigo}</span>
                    </Button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <Pagination
        page={consulta.data?.page ?? 0}
        totalPages={consulta.data?.totalPages ?? 1}
        totalElements={consulta.data?.totalElements ?? 0}
        itemLabel="peças"
        onPageChange={onPageChange}
      />
    </>
  )
}

export function EstoquePage() {
  const [tab, setTab] = useState('todas')
  const [termo, setTermo] = useState('')
  const [busca, setBusca] = useState('')
  const [page, setPage] = useState(0)
  const [formAberto, setFormAberto] = useState(false)
  const [pecaEntrada, setPecaEntrada] = useState<Peca | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  const todas = usePecas(busca, page, 10, tab === 'todas')
  const baixo = useBaixoEstoque(page, 10, tab === 'baixo')

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusca(termo.trim())
    setPage(0)
  }

  return (
    <div>
      <PageHeader
        title="Estoque"
        description="Consulte peças, acompanhe alertas de ruptura e registre entradas."
        actions={
          <Button onClick={() => setFormAberto(true)}>
            <Plus aria-hidden="true" className="size-4" />
            Nova peça
          </Button>
        }
      />

      {sucesso && (
        <FeedbackMessage className="mt-4" tone="success">
          {sucesso}
        </FeedbackMessage>
      )}

      <Tabs
        value={tab}
        onValueChange={(valor) => {
          setTab(valor)
          setPage(0)
        }}
      >
        <TabsList aria-label="Visão do estoque" className="mt-6">
          <TabsTrigger value="todas">Todas as peças</TabsTrigger>
          <TabsTrigger value="baixo">Estoque baixo</TabsTrigger>
        </TabsList>

        <TabsContent value="todas">
          <form
            role="search"
            onSubmit={buscar}
            noValidate
            className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-4 sm:flex-row sm:items-start"
          >
            <div className="flex-1">
              <TextField
                label="Buscar peça"
                placeholder="Código ou descrição"
                value={termo}
                onChange={(event) => setTermo(event.target.value)}
              />
            </div>
            <Button type="submit" className="sm:mt-6">
              <Search aria-hidden="true" className="size-4" />
              Buscar
            </Button>
          </form>

          <div className="mt-4">
            <PecasLista
              consulta={todas}
              vazioTitulo="Nenhuma peça encontrada"
              vazioDescricao="Revise a busca ou cadastre uma nova peça no estoque."
              onEntrada={setPecaEntrada}
              onCadastrar={() => setFormAberto(true)}
              onPageChange={setPage}
            />
          </div>
        </TabsContent>

        <TabsContent value="baixo">
          <p className="text-sm text-ink-muted">
            Peças cuja quantidade atual está abaixo do estoque mínimo definido.
          </p>
          <div className="mt-4">
            <PecasLista
              consulta={baixo}
              vazioTitulo="Nenhuma peça abaixo do mínimo"
              vazioDescricao="O estoque está saudável nesta consulta."
              onEntrada={setPecaEntrada}
              onCadastrar={() => setFormAberto(true)}
              onPageChange={setPage}
            />
          </div>
        </TabsContent>
      </Tabs>

      <PecaFormDialog
        open={formAberto}
        onOpenChange={setFormAberto}
        onCreated={(peca) => setSucesso(`Peça ${peca.codigo} cadastrada.`)}
      />

      <EntradaEstoqueDialog
        peca={pecaEntrada}
        onOpenChange={(open) => {
          if (!open) setPecaEntrada(null)
        }}
        onUpdated={(mensagem) => setSucesso(mensagem)}
      />

      <p className="mt-6 flex items-center gap-2 text-xs text-ink-muted">
        <PackagePlus aria-hidden="true" className="size-4" />
        A baixa de estoque ocorre automaticamente ao confirmar uma OS com peça da loja.
      </p>
    </div>
  )
}

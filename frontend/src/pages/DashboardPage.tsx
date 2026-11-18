import type { ReactNode } from 'react'
import {
  Bike,
  CheckCircle2,
  ChevronRight,
  Package,
  Plus,
  RefreshCw,
} from 'lucide-react'
import type { UseQueryResult } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useBaixoEstoque, useOrdens } from '../api/hooks'
import type { OrdemDeServico, Page, Peca, StatusOS } from '../api/types'
import { Button, buttonVariants } from '../components/ui/Button'
import { EmptyState, ErrorState } from '../components/ui/States'
import { StatusBadge } from '../components/ui/StatusBadge'
import { cn } from '../lib/cn'
import { formatDateTime, formatEspera } from '../lib/format'

const PENDENCIA: Partial<Record<StatusOS, string>> = {
  ABERTA: 'Aguardando início',
  AGUARDANDO_CLIENTE: 'Aprovação ou peça',
}

function hoje(): string {
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date())
}

function horaAtualizada(consultas: { dataUpdatedAt: number }[]): string {
  const marcas = consultas.map((consulta) => consulta.dataUpdatedAt).filter((marca) => marca > 0)
  if (marcas.length === 0) return '—'
  return new Date(Math.max(...marcas)).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

type IndicadorProps = {
  label: string
  hint: string
  valor: number | undefined
  carregando: boolean
  erro: boolean
  to: string
  destaque?: boolean
}

function Indicador({
  label,
  hint,
  valor,
  carregando,
  erro,
  to,
  destaque = false,
}: IndicadorProps) {
  return (
    <Link
      to={to}
      aria-busy={carregando || undefined}
      className={cn(
        'group rounded-2xl px-5 py-4 transition-colors',
        destaque
          ? 'bg-danger-surface/60 hover:bg-danger-surface'
          : 'bg-surface-muted/60 hover:bg-surface-muted',
      )}
    >
      <span className="flex items-baseline gap-2">
        <span className={cn('tnum text-3xl font-semibold tracking-tight', destaque && 'text-danger')}>
          {carregando ? (
            <span
              aria-hidden="true"
              className="inline-block h-7 w-8 animate-pulse rounded bg-ink/10"
            />
          ) : erro ? (
            '—'
          ) : (
            (valor ?? 0)
          )}
        </span>
        <span className={cn('text-sm font-medium', destaque && 'text-danger')}>{label}</span>
      </span>
      <span className="mt-1 block text-xs text-ink-muted">
        {erro ? 'Não foi possível carregar' : hint}
      </span>
    </Link>
  )
}

function SkeletonLinhas({
  quantidade = 3,
  rotulo = 'Carregando motos na oficina…',
}: {
  quantidade?: number
  rotulo?: string
}) {
  return (
    <div role="status" className="divide-y divide-border/60">
      <span className="sr-only">{rotulo}</span>
      {Array.from({ length: quantidade }).map((_, indice) => (
        <div key={indice} className="flex items-center gap-4 py-4">
          <span className="h-4 w-12 animate-pulse rounded bg-surface-muted" />
          <span className="h-4 flex-1 animate-pulse rounded bg-surface-muted" />
          <span className="h-6 w-24 animate-pulse rounded-full bg-surface-muted" />
        </div>
      ))}
    </div>
  )
}

function LinhaDaFila({ os }: { os: OrdemDeServico }) {
  const pendencia = PENDENCIA[os.status]

  return (
    <li>
      <Link
        to={`/ordens-servico/${os.id}`}
        className="group grid grid-cols-[3.25rem_minmax(0,1fr)] items-center gap-x-3 gap-y-1 rounded-2xl px-2 py-3 transition-colors hover:bg-surface-muted/50 sm:grid-cols-[3.75rem_minmax(0,1fr)_auto] sm:gap-x-4"
      >
        <time
          dateTime={os.dataAbertura}
          title={`Aberta em ${formatDateTime(os.dataAbertura)}`}
          className="tnum row-span-2 text-right text-sm font-semibold sm:row-span-1"
        >
          {formatEspera(os.dataAbertura)}
        </time>

        <span className="min-w-0">
          <span className="block truncate text-sm font-medium">
            {os.motoMarca} {os.motoModelo} {os.motoCilindradas && `· ${os.motoCilindradas}cc`}
          </span>
          <span className="mt-0.5 block truncate text-xs text-ink-muted">
            {os.clienteNome}
            {pendencia ? ` · ${pendencia}` : ''}
          </span>
        </span>

        <span className="col-start-2 flex items-center gap-3 sm:col-start-3 sm:row-start-1">
          <StatusBadge status={os.status} />
          <ChevronRight
            aria-hidden="true"
            className="hidden size-4 shrink-0 text-ink-muted transition-transform group-hover:translate-x-0.5 sm:block"
          />
        </span>
      </Link>
    </li>
  )
}

type PainelProps = {
  consultas: UseQueryResult<Page<OrdemDeServico>>[]
  descricao: string
}

function QuadroDeBaias({ consultas, descricao }: PainelProps) {
  const lista = consultas.flatMap((consulta) => consulta.data?.content ?? [])
  lista.sort((a, b) => new Date(a.dataAbertura).getTime() - new Date(b.dataAbertura).getTime())

  const ordens = lista
  const carregando = consultas.some((consulta) => consulta.isLoading)
  const erro = consultas.every((consulta) => consulta.isError)
  const total = consultas.reduce((soma, consulta) => soma + (consulta.data?.totalElements ?? 0), 0)
  const refetch = () => consultas.forEach((consulta) => void consulta.refetch())

  const visiveis = ordens.slice(0, 8)

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 id="motos-na-oficina" className="text-xl font-semibold tracking-tight">
            Motos na oficina
          </h2>
          <p className="mt-1 text-sm text-ink-muted">{descricao}</p>
        </div>
        <Link
          to="/ordens-servico"
          className="text-sm font-medium text-ink underline decoration-border-strong underline-offset-4 transition-colors hover:decoration-ink"
        >
          Ver todas
        </Link>
      </div>

      <div className="mt-2">
        {carregando && <SkeletonLinhas quantidade={4} />}

        {erro && (
          <ErrorState
            title="Não foi possível carregar as motos na oficina"
            message="A lista não chegou. Verifique a conexão e tente de novo."
            onRetry={refetch}
          />
        )}

        {!carregando && !erro && ordens.length === 0 && (
          <EmptyState
            icon={Bike}
            title="Pátio vazio"
            description="Nenhuma moto na oficina agora. Abra uma OS quando a próxima chegar."
            action={
              <Link to="/ordens-servico?nova=1" className={buttonVariants()}>
                <Plus aria-hidden="true" className="size-4" />
                Abrir OS
              </Link>
            }
          />
        )}

        {!carregando && !erro && ordens.length > 0 && (
          <>
            <ul className="divide-y divide-border/60">
              {visiveis.map((os) => (
                <LinhaDaFila key={os.id} os={os} />
              ))}
            </ul>
            {total > visiveis.length && (
              <p className="pt-3 text-xs text-ink-muted">
                Mostrando as {visiveis.length} que estão há mais tempo no pátio, de {total}.
              </p>
            )}
          </>
        )}
      </div>
    </div>
  )
}

function EstoqueCritico({ consulta }: { consulta: UseQueryResult<Page<Peca>> }) {
  const pecas: Peca[] = consulta.data?.content ?? []
  const total = consulta.data?.totalElements ?? 0

  return (
    <section aria-labelledby="estoque-critico" className="rounded-3xl bg-surface p-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 id="estoque-critico" className="text-xl font-semibold tracking-tight">
            Peças em ruptura
          </h2>
          <p className="mt-1 text-sm text-ink-muted">Cada uma pode parar um serviço.</p>
        </div>
        <Link
          to="/estoque"
          className="text-sm font-medium text-ink underline decoration-border-strong underline-offset-4 transition-colors hover:decoration-ink"
        >
          Ver estoque
        </Link>
      </div>

      <div className="mt-4">
        {consulta.isLoading && (
          <SkeletonLinhas quantidade={2} rotulo="Carregando peças em ruptura…" />
        )}

        {consulta.isError && (
          <ErrorState
            title="Não foi possível carregar o estoque"
            message="Os alertas de ruptura não chegaram."
            onRetry={() => void consulta.refetch()}
          />
        )}

        {consulta.data && pecas.length === 0 && (
          <div className="py-8 text-center">
            <CheckCircle2 aria-hidden="true" className="mx-auto size-7 text-success" />
            <p className="mt-2 text-sm font-medium">Estoque saudável</p>
            <p className="mt-1 text-sm text-ink-muted">
              Nenhuma peça abaixo do estoque mínimo.
            </p>
          </div>
        )}

        {pecas.length > 0 && (
          <>
            <ul className="divide-y divide-border/60">
              {pecas.map((peca) => (
                <li key={peca.id} className="flex items-center gap-3 py-4">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-danger-surface text-danger">
                    <Package aria-hidden="true" className="size-[18px]" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-2 text-sm font-medium">{peca.descricao}</p>
                    <p className="tnum text-xs text-ink-muted">
                      {peca.codigo} · {peca.quantidadeEstoque}/{peca.estoqueMinimo} un.
                    </p>
                  </div>
                  <Link
                    to="/estoque"
                    aria-label={`Repor ${peca.descricao} no estoque`}
                    className="rounded-full bg-danger-surface px-3 py-1 text-xs font-semibold text-danger transition-colors hover:bg-danger-surface/70"
                  >
                    Repor
                  </Link>
                </li>
              ))}
            </ul>
            {total > pecas.length && (
              <p className="pt-2 text-xs text-ink-muted">
                +{total - pecas.length} peça(s) em situação crítica.
              </p>
            )}
          </>
        )}
      </div>
    </section>
  )
}

function Metadados({ children }: { children: ReactNode }) {
  return <p className="text-xs text-ink-muted">{children}</p>
}

export function DashboardPage() {
  const abertas = useOrdens(undefined, 'ABERTA', 0, 20)
  const emAndamento = useOrdens(undefined, 'EM_ANDAMENTO', 0, 20)
  const aguardando = useOrdens(undefined, 'AGUARDANDO_CLIENTE', 0, 20)
  const concluidas = useOrdens(undefined, 'CONCLUIDA', 0, 1)
  const estoque = useBaixoEstoque(0, 4)

  const consultasAtivas = [abertas, emAndamento, aguardando]
  const atualizadoEm = horaAtualizada([...consultasAtivas, concluidas, estoque])

  function recarregar() {
    consultasAtivas.forEach((consulta) => void consulta.refetch())
    void concluidas.refetch()
    void estoque.refetch()
  }

  const naOficina =
    (abertas.data?.totalElements ?? 0) +
    (emAndamento.data?.totalElements ?? 0) +
    (aguardando.data?.totalElements ?? 0)

  const erroAtivo = consultasAtivas.some((consulta) => consulta.isError)

  return (
    <div>
      <header className="flex flex-wrap items-end justify-between gap-6">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Painel da oficina
          </h1>
          <p className="mt-2 max-w-xl text-sm leading-6 text-ink-muted">
            O pátio em tempo real: quais motos estão aqui, há quanto tempo esperam e o que
            trava cada uma.
          </p>
          <Metadados>
            {hoje()} · atualizado às {atualizadoEm}
          </Metadados>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button variant="secondary" size="sm" onClick={recarregar} className="rounded-full">
            <RefreshCw aria-hidden="true" className="size-4" />
            Atualizar
          </Button>
          <Link
            to="/ordens-servico?nova=1"
            className={cn(buttonVariants(), 'rounded-full px-5 font-semibold')}
          >
            <Plus aria-hidden="true" className="size-4" />
            Abrir OS
          </Link>
        </div>
      </header>

      <section aria-labelledby="patio" className="mt-10">
        <h2 id="patio" className="text-sm font-semibold">
          Pátio
        </h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-3">
          <Indicador
            label="Na oficina"
            hint={
              erroAtivo
                ? ''
                : `${emAndamento.data?.totalElements ?? 0} em execução · ${
                    aguardando.data?.totalElements ?? 0
                  } esperando o cliente · ${abertas.data?.totalElements ?? 0} aguardando início`
            }
            valor={erroAtivo ? undefined : naOficina}
            carregando={consultasAtivas.some((consulta) => consulta.isLoading)}
            erro={erroAtivo}
            to="/ordens-servico"
          />
          <Indicador
            label="Prontas"
            hint="Serviço concluído, aguardando fechamento"
            valor={concluidas.data?.totalElements}
            carregando={concluidas.isLoading}
            erro={concluidas.isError}
            to="/ordens-servico?status=CONCLUIDA"
          />
          <Indicador
            label="Peças em ruptura"
            hint="Abaixo do estoque mínimo"
            valor={estoque.data?.totalElements}
            carregando={estoque.isLoading}
            erro={estoque.isError}
            to="/estoque"
            destaque
          />
        </div>
      </section>

      <div className="mt-6 grid items-start gap-6 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <section aria-labelledby="motos-na-oficina" className="rounded-3xl bg-surface p-6">
          <QuadroDeBaias
            consultas={consultasAtivas}
            descricao="Da que está há mais tempo para a que acabou de chegar."
          />
        </section>

        <EstoqueCritico consulta={estoque} />
      </div>
    </div>
  )
}

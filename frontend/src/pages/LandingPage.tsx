import {
  ArrowRight,
  BarChart3,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  ClipboardList,
  Package,
  ShieldCheck,
  Users,
  Wrench,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { buttonVariants } from '../components/ui/Button'
import { StatusBadge } from '../components/ui/StatusBadge'
import { cn } from '../lib/cn'
import logoMark from '../assets/logo-mark.png'
import { depoimentoMock, perguntasFrequentesMock } from './landingContent'

function Marca() {
  return (
    <Link to="/" className="flex shrink-0 items-center gap-2.5" aria-label="MotoServices, início">
      <img src={logoMark} alt="" className="size-10" />
      <span className="flex flex-col leading-tight">
        <span className="text-[15px] font-semibold tracking-tight">MotoServices</span>
        <span className="text-[10px] font-semibold tracking-[0.16em] text-ink-muted uppercase">
          Oficina
        </span>
      </span>
    </Link>
  )
}

function PreviewPainel() {
  return (
    <div className="relative mx-auto w-full max-w-[560px]" aria-hidden="true">
      <div className="absolute -inset-6 rounded-[40px] bg-primary-surface/80 blur-2xl" />

      <div className="relative overflow-hidden rounded-[28px] border border-border bg-surface shadow-[0_24px_80px_-32px_rgba(28,25,23,0.35)]">
        <div className="flex items-center gap-2 border-b border-border/70 bg-surface px-4 py-3">
          <span className="size-2 rounded-full bg-border-strong" />
          <span className="size-2 rounded-full bg-border-strong" />
          <span className="size-2 rounded-full bg-border-strong" />
          <span className="ml-3 rounded-full bg-surface-muted px-3 py-1 text-[10px] text-ink-muted">
            motoservices / painel
          </span>
        </div>

        <div className="grid min-w-0 grid-cols-[64px_minmax(0,1fr)] sm:grid-cols-[104px_minmax(0,1fr)]">
          <aside className="border-r border-border/70 bg-surface-muted/40 p-2.5">
            <div className="flex items-center gap-2 rounded-lg px-2 py-2">
              <span className="grid size-6 place-items-center rounded-md bg-primary text-[10px] font-bold text-on-primary">
                M
              </span>
              <span className="hidden text-[11px] font-semibold sm:block">MotoServices</span>
            </div>
            <div className="mt-3 space-y-1 text-[10px]">
              <span className="flex items-center gap-1.5 rounded-md bg-surface px-2 py-1.5 font-semibold shadow-sm">
                <ClipboardList className="size-3" /> Painel
              </span>
              <span className="flex items-center gap-1.5 rounded-md px-2 py-1.5 text-ink-muted">
                <Wrench className="size-3" /> Ordens
              </span>
              <span className="flex items-center gap-1.5 rounded-md px-2 py-1.5 text-ink-muted">
                <Users className="size-3" /> Clientes
              </span>
              <span className="flex items-center gap-1.5 rounded-md px-2 py-1.5 text-ink-muted">
                <Package className="size-3" /> Estoque
              </span>
            </div>
          </aside>

          <div className="min-w-0 p-3.5 sm:p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-[9px] font-semibold tracking-[0.14em] text-ink-muted uppercase">
                  Painel da oficina
                </p>
                <p className="mt-1 text-lg font-semibold tracking-tight sm:text-xl">Bom dia!</p>
              </div>
              <span className="rounded-full bg-primary px-2.5 py-1 text-[9px] font-semibold text-on-primary">
                Abrir OS
              </span>
            </div>

            <div className="mt-3 grid grid-cols-3 gap-1.5">
              {[
                { rotulo: 'Na oficina', valor: '4', tom: 'bg-surface-muted' },
                { rotulo: 'Prontas', valor: '2', tom: 'bg-primary-surface' },
                { rotulo: 'Sem estoque', valor: '1', tom: 'bg-danger-surface' },
              ].map((item) => (
                <div key={item.rotulo} className={cn('rounded-lg p-2', item.tom)}>
                  <p className="text-base font-semibold tracking-tight">{item.valor}</p>
                  <p className="text-[8px] text-ink-muted">{item.rotulo}</p>
                </div>
              ))}
            </div>

            <div className="mt-3 rounded-xl border border-border/70 p-2.5">
              <div className="flex items-center justify-between text-[10px] font-semibold">
                <span>Motos na oficina</span>
                <span className="font-normal text-ink-muted">Ver todas</span>
              </div>
              <ul className="mt-1.5 divide-y divide-border/60">
                {[
                  { os: '#128', moto: 'Honda CG 160', cliente: 'Maria Silva', status: 'EM_ANDAMENTO' as const },
                  { os: '#127', moto: 'Yamaha Fazer 250', cliente: 'João Costa', status: 'AGUARDANDO_CLIENTE' as const },
                  { os: '#126', moto: 'Honda Biz 125', cliente: 'Ana Souza', status: 'ABERTA' as const },
                ].map((linha) => (
                  <li key={linha.os} className="flex items-center gap-2 py-1.5">
                    <span className="w-8 text-[8px] font-semibold text-ink-muted">{linha.os}</span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[9px] font-medium">{linha.moto}</span>
                      <span className="block truncate text-[8px] text-ink-muted">{linha.cliente}</span>
                    </span>
                    <span className="scale-90 origin-right">
                      <StatusBadge status={linha.status} />
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </div>

      <div className="absolute -right-3 -bottom-5 hidden w-40 rounded-2xl border border-border bg-surface p-3 shadow-xl sm:-right-5 sm:block sm:w-48">
        <div className="flex items-center gap-2">
          <span className="grid size-7 place-items-center rounded-full bg-danger-surface text-danger">
            <Package className="size-3.5" />
          </span>
          <div>
            <p className="text-[9px] font-semibold">Peça em falta</p>
            <p className="text-[8px] text-ink-muted">Pastilha · 0/2 un.</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function ItemProva({ children }: { children: string }) {
  return (
    <li className="flex items-start gap-2 text-sm text-ink-muted">
      <CheckCircle2 aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-primary" />
      <span>{children}</span>
    </li>
  )
}

const recursos = [
  {
    icone: ClipboardCheck,
    titulo: 'Uma OS do começo ao fim',
    texto:
      'Abra a ordem, monte o orçamento, acompanhe o status e feche com o valor combinado. Cada mudança fica no histórico.',
  },
  {
    icone: Package,
    titulo: 'Estoque que não some',
    texto:
      'O estoque é atualizado quando o cliente confirma. Cancelou ou removeu o serviço? A peça volta sozinha.',
  },
  {
    icone: Users,
    titulo: 'Cliente, moto e histórico juntos',
    texto:
      'Vincule motos ao cliente na primeira OS e consulte o histórico por cliente ou moto. Veja também quem precisa revisar.',
  },
]

const etapas = [
  {
    titulo: 'Identifique cliente e moto',
    texto: 'A moto precisa existir no catálogo. O vínculo com o cliente é criado na primeira OS.',
  },
  {
    titulo: 'Monte o orçamento',
    texto:
      'Adicione revisões e trocas de peça. O preço se ajusta pela cilindrada e o desconto entra sozinho, sem calculadora.',
  },
  {
    titulo: 'Confirme, acompanhe e feche',
    texto:
      'A confirmação atualiza o estoque. No fechamento, o valor fica registrado, o pagamento é anotado e a próxima revisão é agendada.',
  },
]

export function LandingPage() {
  return (
    <div className="min-h-dvh overflow-x-hidden bg-canvas">
      <a
        href="#conteudo"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:rounded-full focus:bg-surface focus:px-4 focus:py-2 focus:text-sm focus:text-ink focus:shadow-lg"
      >
        Pular para o conteúdo
      </a>

      <header className="sticky top-0 z-40 border-b border-border/70 bg-canvas/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-6">
          <Marca />
          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {[
              ['Recursos', '#recursos'],
              ['Como funciona', '#como-funciona'],
              ['Relatórios', '#relatorios'],
              ['Perguntas', '#faq'],
            ].map(([rotulo, href]) => (
              <a
                key={href}
                href={href}
                className="rounded-full px-3 py-2 text-sm text-ink-muted transition-colors hover:bg-surface-muted hover:text-ink"
              >
                {rotulo}
              </a>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <Link
              to="/painel"
              className={buttonVariants({ variant: 'accent', size: 'sm' })}
            >
              Abrir painel
              <ArrowRight aria-hidden="true" className="size-4" />
            </Link>
          </div>
        </div>
      </header>

      <main id="conteudo">
        <section className="relative overflow-hidden">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -top-40 -right-40 size-[560px] rounded-full bg-primary-surface/70 blur-3xl"
          />
          <div className="relative mx-auto grid max-w-6xl gap-14 px-4 pt-16 pb-20 sm:px-6 lg:grid-cols-[1.02fr_1fr] lg:items-center lg:pt-24 lg:pb-28">
            <div className="min-w-0">
              <p className="inline-flex items-center gap-2 rounded-full border border-border bg-surface px-3 py-1.5 text-xs font-medium text-ink-muted">
                <span className="size-1.5 rounded-full bg-primary" />
                Para oficinas de moto
              </p>
              <h1 className="mt-6 text-balance text-5xl font-semibold tracking-[-0.04em] sm:text-6xl lg:text-[68px] lg:leading-[1.02]">
                A oficina inteira
                <span className="block text-primary">em uma tela.</span>
              </h1>
              <p className="mt-6 max-w-xl text-lg leading-8 text-ink-muted">
                Chega de caderno, planilha e WhatsApp. Abra a OS, controle as peças e feche com o
                valor certo, com o histórico de cada moto sempre à mão.
              </p>
              <div className="mt-8 flex flex-wrap items-center gap-3">
                <Link
                  to="/painel"
                  className={buttonVariants({ variant: 'primary', size: 'lg' })}
                >
                  Abrir minha primeira OS
                  <ArrowRight aria-hidden="true" className="size-4" />
                </Link>
                <a
                  href="#como-funciona"
                  className={cn(buttonVariants({ variant: 'secondary', size: 'lg' }), 'rounded-full')}
                >
                  Ver como funciona
                </a>
              </div>
              <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-xs text-ink-muted">
                <li className="flex items-center gap-1.5">
                  <ShieldCheck aria-hidden="true" className="size-4 text-primary" />
                  Estoque atualizado sozinho
                </li>
                <li className="flex items-center gap-1.5">
                  <CalendarClock aria-hidden="true" className="size-4 text-primary" />
                  Próxima revisão agendada
                </li>
                <li className="flex items-center gap-1.5">
                  <ClipboardCheck aria-hidden="true" className="size-4 text-primary" />
                  Valor combinado não muda
                </li>
              </ul>
            </div>

            <div className="min-w-0">
              <PreviewPainel />
            </div>
          </div>
        </section>

        <section aria-label="O que o MotoServices resolve" className="border-y border-border/70 bg-surface/60">
          <div className="mx-auto grid max-w-6xl gap-6 px-4 py-8 sm:grid-cols-3 sm:px-6">
            {[
              ['Orçamento certo', 'Na primeira vez, sem calculadora nem planilha.'],
              ['Estoque em dia', 'A peça sai e volta sozinha, conforme a OS.'],
              ['Histórico à mão', 'Cada mudança de status fica registrada com data e hora.'],
            ].map(([titulo, texto]) => (
              <div key={titulo}>
                <p className="text-sm font-semibold">{titulo}</p>
                <p className="mt-1 text-sm leading-6 text-ink-muted">{texto}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="recursos" className="scroll-mt-24">
          <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:py-28">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold tracking-[0.16em] text-primary uppercase">
                O que muda no dia a dia
              </p>
              <h2 className="mt-3 text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
                Menos retrabalho. Mais moto pronta.
              </h2>
              <p className="mt-4 text-base leading-7 text-ink-muted">
                O sistema segue o fluxo real da oficina: diagnosticar, orçar, executar e fechar.
              </p>
            </div>

            <div className="mt-12 grid gap-4 md:grid-cols-3">
              {recursos.map(({ icone: Icone, titulo, texto }) => (
                <article
                  key={titulo}
                  className="rounded-3xl border border-border bg-surface p-6 transition-colors hover:border-border-strong"
                >
                  <span className="grid size-11 place-items-center rounded-2xl bg-primary-surface text-primary">
                    <Icone aria-hidden="true" className="size-5" />
                  </span>
                  <h3 className="mt-5 text-lg font-semibold tracking-tight">{titulo}</h3>
                  <p className="mt-2 text-sm leading-6 text-ink-muted">{texto}</p>
                </article>
              ))}
            </div>

            <article
              id="relatorios"
              className="mt-4 grid scroll-mt-24 gap-8 rounded-3xl bg-surface p-6 sm:p-8 lg:grid-cols-[1fr_0.9fr] lg:items-center"
            >
              <div>
                <span className="grid size-11 place-items-center rounded-2xl bg-primary-surface text-primary">
                  <BarChart3 aria-hidden="true" className="size-5" />
                </span>
                <h3 className="mt-5 text-2xl font-semibold tracking-tight">
                  Relatórios que fecham o mês
                </h3>
                <p className="mt-2 max-w-lg text-sm leading-6 text-ink-muted">
                  Faturamento por período, ticket médio, serviços mais realizados e clientes com
                  mais atendimentos. Tudo calculado pelo valor combinado no fechamento, não pelo
                  preço de hoje.
                </p>
                <Link
                  to="/relatorios"
                  className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-primary hover:underline"
                >
                  Ver relatórios no painel
                  <ArrowRight aria-hidden="true" className="size-4" />
                </Link>
              </div>
              <div className="rounded-2xl bg-canvas p-4">
                <div className="grid gap-2 sm:grid-cols-2">
                  {[
                    ['Faturamento', 'R$ 12.480,00'],
                    ['Ticket médio', 'R$ 520,00'],
                    ['Serviço top', 'Revisão 10k'],
                    ['OS fechadas', '24'],
                  ].map(([rotulo, valor]) => (
                    <div key={rotulo} className="rounded-xl bg-surface p-4">
                      <p className="text-xs text-ink-muted">{rotulo}</p>
                      <p className="mt-1 text-lg font-semibold tracking-tight">{valor}</p>
                    </div>
                  ))}
                </div>
                <p className="mt-2 text-xs text-ink-muted">Dados de exemplo</p>
              </div>
            </article>
          </div>
        </section>

        <section id="como-funciona" className="scroll-mt-24 bg-surface/60">
          <div className="mx-auto grid max-w-6xl gap-14 px-4 py-20 sm:px-6 lg:grid-cols-[0.9fr_1.1fr] lg:py-28">
            <div>
              <p className="text-xs font-semibold tracking-[0.16em] text-primary uppercase">
                Como funciona
              </p>
              <h2 className="mt-3 text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
                Do primeiro diagnóstico ao comprovante.
              </h2>
              <p className="mt-4 max-w-md text-base leading-7 text-ink-muted">
                Em cada etapa, você sabe onde a moto está e o que falta para entregar.
              </p>
              <Link
                to="/painel"
                className={cn(buttonVariants(), 'mt-7 rounded-full')}
              >
                Começar pelo painel
                <ArrowRight aria-hidden="true" className="size-4" />
              </Link>
            </div>

            <ol className="space-y-3">
              {etapas.map((etapa, indice) => (
                <li
                  key={etapa.titulo}
                  className="flex gap-5 rounded-2xl border border-border bg-surface p-5"
                >
                  <span className="grid size-9 shrink-0 place-items-center rounded-full bg-primary-surface text-sm font-semibold text-primary">
                    {String(indice + 1).padStart(2, '0')}
                  </span>
                  <div>
                    <h3 className="font-semibold">{etapa.titulo}</h3>
                    <p className="mt-1 text-sm leading-6 text-ink-muted">{etapa.texto}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:py-28">
          <div className="grid gap-10 lg:grid-cols-[1fr_0.9fr] lg:items-center">
            <div>
              <p className="text-xs font-semibold tracking-[0.16em] text-primary uppercase">
                Sem surpresa no fechamento
              </p>
              <h2 className="mt-3 text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
                O que o cliente combinou é o que ele paga.
              </h2>
              <p className="mt-4 text-base leading-7 text-ink-muted">
                O orçamento é calculado serviço a serviço e registrado na OS no fechamento. Mudou o
                preço da peça depois? A OS antiga continua com o valor acordado.
              </p>
              <ul className="mt-6 space-y-3">
                <ItemProva>Peça da loja entra no total; peça do cliente cobra só a mão de obra.</ItemProva>
                <ItemProva>Cinco ou mais peças da loja? O desconto de 5% entra sozinho.</ItemProva>
                <ItemProva>Cancelou antes de o cliente aprovar o serviço? O estoque volta automaticamente.</ItemProva>
              </ul>
            </div>

            <div className="rounded-3xl border border-border bg-surface p-5">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-ink-muted">OS #128</p>
                  <p className="font-semibold">Honda CG 160 · Maria Silva</p>
                </div>
                <StatusBadge status="FECHADA" />
              </div>
              <dl className="mt-5 space-y-2 text-sm">
                {[
                  ['Peças', 'R$ 120,00'],
                  ['Mão de obra', 'R$ 230,00'],
                  ['Desconto', '− R$ 0,00'],
                ].map(([rotulo, valor]) => (
                  <div key={rotulo} className="flex justify-between gap-4">
                    <dt className="text-ink-muted">{rotulo}</dt>
                    <dd className="tnum">{valor}</dd>
                  </div>
                ))}
                <div className="flex justify-between gap-4 border-t border-border pt-3 text-base font-semibold">
                  <dt>Total</dt>
                  <dd className="tnum">R$ 350,00</dd>
                </div>
              </dl>
              <div className="mt-4 flex items-center justify-between rounded-xl bg-surface-muted px-3 py-2 text-xs">
                <span className="text-ink-muted">Pagamento</span>
                <span className="font-semibold">PIX</span>
              </div>
              <div className="mt-3 flex items-center gap-2 text-xs text-success">
                <CheckCircle2 aria-hidden="true" className="size-4" />
                Valor combinado não muda após o fechamento
              </div>
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold tracking-[0.16em] text-primary uppercase">
              Quem usa, conta
            </p>
            <h2 className="mt-3 text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
              Depoimento
            </h2>
            <p className="mt-2 text-xs text-ink-muted">
              Conteúdo de exemplo — substituir antes de publicar.
            </p>
            {/* TODO: trocar o mock de landingContent.ts por depoimento real */}
            <figure className="mt-8 rounded-3xl border border-border bg-surface p-6 sm:p-8">
              <blockquote className="text-xl leading-8 text-ink">
                “{depoimentoMock.citacao}”
              </blockquote>
              <figcaption className="mt-4 text-sm text-ink-muted">
                {depoimentoMock.assinatura}
              </figcaption>
            </figure>
          </div>
        </section>

        <section id="faq" className="scroll-mt-24 bg-surface/60">
          <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:py-28">
            <div className="max-w-2xl">
              <p className="text-xs font-semibold tracking-[0.16em] text-primary uppercase">
                Perguntas frequentes
              </p>
              <h2 className="mt-3 text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
                O que você precisa saber.
              </h2>
              <p className="mt-2 text-xs text-ink-muted">
                Conteúdo de exemplo — respostas a confirmar antes de publicar.
              </p>
            </div>

            {/* TODO: revisar as respostas mockadas de landingContent.ts com o time de produto */}
            <div className="mt-10 max-w-3xl space-y-3">
              {perguntasFrequentesMock.map(({ pergunta, resposta }) => (
                <details
                  key={pergunta}
                  className="rounded-3xl border border-border bg-surface p-5"
                >
                  <summary className="cursor-pointer font-semibold">{pergunta}</summary>
                  <p className="mt-2 text-sm leading-6 text-ink-muted">{resposta}</p>
                </details>
              ))}
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
          <div className="relative overflow-hidden rounded-[32px] bg-primary px-6 py-14 text-on-primary sm:px-12 sm:py-16">
            <div
              aria-hidden="true"
              className="absolute -top-24 -right-16 size-72 rounded-full bg-accent/20 blur-3xl"
            />
            <div className="relative max-w-2xl">
              <h2 className="text-balance text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
                A próxima moto já pode entrar com a OS aberta.
              </h2>
              <p className="mt-4 text-base leading-7 text-on-primary/80">
                Abra o painel, cadastre cliente e moto e monte o primeiro orçamento em poucos
                minutos. O estoque e o histórico começam a trabalhar junto com você.
              </p>
              <Link
                to="/painel"
                className={cn(buttonVariants({ variant: 'accent', size: 'lg' }), 'mt-8 rounded-full')}
              >
                Cadastrar a primeira moto
                <ArrowRight aria-hidden="true" className="size-4" />
              </Link>
              {/* TODO: confirmar texto de preço/teste grátis */}
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border/70">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-10 text-sm text-ink-muted sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <Marca />
          <p>Sistema de gestão de ordens de serviço para oficinas de motocicletas.</p>
          <div className="flex flex-wrap items-center gap-4">
            {/* TODO: definir URLs de contato e de termos e privacidade */}
            <a href="#" className="font-medium text-ink hover:underline">
              Contato
            </a>
            <a href="#" className="font-medium text-ink hover:underline">
              Termos e privacidade
            </a>
            <Link to="/painel" className="font-medium text-ink hover:underline">
              Abrir painel
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}

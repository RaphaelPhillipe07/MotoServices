import { useQueryClient } from '@tanstack/react-query'
import { LogOut } from 'lucide-react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import logoMark from '../../assets/logo-mark.png'
import { limparSessao, useSessao } from '../../lib/session'
import { navigation } from './navigation'

const rotuloPapel = {
  ADMIN: 'Administrador',
  ATENDENTE: 'Atendente',
  MECANICO: 'Mecânico',
} as const

export function AppLayout() {
  const sessao = useSessao()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const itens = navigation.filter(
    (item) => !item.papeis || (sessao && item.papeis.includes(sessao.usuario.papel)),
  )

  function sair() {
    limparSessao()
    queryClient.clear()
    navigate('/entrar', { replace: true })
  }

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[260px_minmax(0,1fr)]">
      <a
        href="#conteudo"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:rounded-full focus:bg-surface focus:px-4 focus:py-2 focus:text-sm focus:text-ink focus:shadow-lg"
      >
        Pular para o conteúdo
      </a>

      <aside className="sticky top-0 z-30 bg-surface/90 backdrop-blur lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:bg-surface lg:px-5 lg:py-7">
        <div className="mx-auto w-full max-w-6xl px-4 py-3 lg:mx-0 lg:flex lg:h-full lg:max-w-none lg:flex-col lg:px-0 lg:py-0">
          <div className="flex items-center justify-between gap-3">
            <Link to="/painel" className="flex shrink-0 items-center gap-2.5">
              <img src={logoMark} alt="" className="size-10" />
              <span className="flex flex-col leading-tight">
                <span className="text-[15px] font-semibold tracking-tight">MotoServices</span>
                <span className="text-[10px] font-semibold tracking-[0.16em] text-ink-muted uppercase">
                  Oficina
                </span>
              </span>
            </Link>

            <button
              type="button"
              onClick={sair}
              className="inline-flex items-center gap-1.5 rounded-full px-3 py-2 text-xs font-semibold text-ink-muted transition-colors hover:bg-surface-muted hover:text-ink lg:hidden"
            >
              <LogOut aria-hidden="true" className="size-4" />
              Sair
            </button>
          </div>

          <nav aria-label="Principal" className="mt-3 lg:mt-8">
            <ul className="flex gap-1 overflow-x-auto pb-1 lg:flex-col lg:space-y-1 lg:overflow-visible lg:pb-0">
              {itens.map(({ to, label, icon: Icon, end }) => (
                <li key={to} className="shrink-0">
                  <NavLink
                    to={to}
                    end={end}
                    className={({ isActive }) =>
                      [
                        'flex items-center gap-2 rounded-full px-3.5 py-2 text-sm whitespace-nowrap transition-colors lg:gap-3 lg:px-4 lg:py-2.5',
                        isActive
                          ? 'bg-surface-muted font-semibold text-ink'
                          : 'text-ink-muted hover:bg-surface-muted/60 hover:text-ink',
                      ].join(' ')
                    }
                  >
                    {({ isActive }) => (
                      <>
                        <Icon
                          aria-hidden="true"
                          className={isActive ? 'size-[18px] text-primary' : 'size-[18px]'}
                        />
                        {label}
                      </>
                    )}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>

          <div className="mt-auto hidden lg:block">
            <div className="rounded-2xl bg-surface-muted/70 p-4">
              <p className="text-sm font-semibold">Atendimento</p>
              <p className="mt-1 text-xs leading-5 text-ink-muted">
                Abra uma OS em poucos passos e acompanhe a fila do dia.
              </p>
              <Link
                to="/ordens-servico?nova=1"
                className="mt-3 inline-flex rounded-full bg-primary-surface px-3.5 py-2 text-xs font-semibold text-primary transition-colors hover:bg-primary-surface/80"
              >
                Nova OS
              </Link>
            </div>

            {sessao && (
              <div className="mt-3 rounded-2xl border border-border p-4">
                <p className="truncate text-sm font-semibold">{sessao.usuario.nome}</p>
                <p className="mt-0.5 truncate text-[11px] text-ink-muted">
                  {rotuloPapel[sessao.usuario.papel]} · @{sessao.usuario.username}
                </p>
                <button
                  type="button"
                  onClick={sair}
                  className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
                >
                  <LogOut aria-hidden="true" className="size-3.5" />
                  Sair
                </button>
              </div>
            )}

            <p className="mt-6 text-[11px] text-ink-muted">MotoServices · v0.5</p>
          </div>
        </div>
      </aside>

      <div className="flex min-h-dvh min-w-0 flex-col">
        <main
          id="conteudo"
          tabIndex={-1}
          className="mx-auto w-full max-w-6xl flex-1 px-5 py-8 sm:px-6 lg:px-10 lg:py-10"
        >
          <Outlet />
        </main>

        <footer className="mx-auto w-full max-w-6xl px-5 pb-8 text-xs text-ink-muted sm:px-6 lg:px-10">
          MotoServices — gestão de oficina de motocicletas.
        </footer>
      </div>
    </div>
  )
}

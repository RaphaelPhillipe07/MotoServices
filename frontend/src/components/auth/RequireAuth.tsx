import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useSessao } from '../../lib/session'

/**
 * Protege as rotas do painel: sem sessão válida, redireciona para o login
 * preservando o destino original.
 */
export function RequireAuth() {
  const sessao = useSessao()
  const localizacao = useLocation()

  if (!sessao) {
    return (
      <Navigate
        to="/entrar"
        replace
        state={{ de: `${localizacao.pathname}${localizacao.search}` }}
      />
    )
  }

  return <Outlet />
}

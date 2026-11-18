import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { RequireAuth } from './components/auth/RequireAuth'
import { AppLayout } from './components/layout/AppLayout'
import { LoadingState } from './components/ui/States'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'

const DashboardPage = lazy(() =>
  import('./pages/DashboardPage').then((modulo) => ({ default: modulo.DashboardPage })),
)
const OrdensPage = lazy(() =>
  import('./pages/ordens/OrdensPage').then((modulo) => ({ default: modulo.OrdensPage })),
)
const OrdemDetalhePage = lazy(() =>
  import('./pages/ordens/OrdemDetalhePage').then((modulo) => ({ default: modulo.OrdemDetalhePage })),
)
const ClientesPage = lazy(() =>
  import('./pages/clientes/ClientesPage').then((modulo) => ({ default: modulo.ClientesPage })),
)
const ClienteDetalhePage = lazy(() =>
  import('./pages/clientes/ClienteDetalhePage').then((modulo) => ({
    default: modulo.ClienteDetalhePage,
  })),
)
const MotosPage = lazy(() =>
  import('./pages/motos/MotosPage').then((modulo) => ({ default: modulo.MotosPage })),
)
const EstoquePage = lazy(() =>
  import('./pages/estoque/EstoquePage').then((modulo) => ({ default: modulo.EstoquePage })),
)
const RelatoriosPage = lazy(() =>
  import('./pages/relatorios/RelatoriosPage').then((modulo) => ({
    default: modulo.RelatoriosPage,
  })),
)

function CarregandoRota() {
  return (
    <div className="grid min-h-dvh place-items-center bg-canvas">
      <LoadingState label="Carregando painel…" />
    </div>
  )
}

export function AppRoutes() {
  return (
    <Suspense fallback={<CarregandoRota />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/entrar" element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<AppLayout />}>
            <Route path="painel" element={<DashboardPage />} />
            <Route path="ordens-servico" element={<OrdensPage />} />
            <Route path="ordens-servico/:osId" element={<OrdemDetalhePage />} />
            <Route path="clientes" element={<ClientesPage />} />
            <Route path="clientes/:clienteId" element={<ClienteDetalhePage />} />
            <Route path="motos" element={<MotosPage />} />
            <Route path="estoque" element={<EstoquePage />} />
            <Route path="relatorios" element={<RelatoriosPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  )
}

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { limparSessao, salvarSessao } from '../lib/session'

type OpcoesRender = {
  route?: string
  /** Semeia uma sessão ADMIN válida (padrão) para renderizar rotas protegidas. */
  autenticado?: boolean
}

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', autenticado = true }: OpcoesRender = {},
) {
  if (autenticado) {
    salvarSessao({
      accessToken: 'token-de-teste',
      tokenType: 'Bearer',
      expiresIn: 3600,
      usuario: { id: 1, username: 'admin', nome: 'Administrador', papel: 'ADMIN' },
    })
  } else {
    limparSessao()
  }

  const testQueryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={testQueryClient}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  )
}

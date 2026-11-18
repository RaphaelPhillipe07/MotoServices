import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { axe } from 'vitest-axe'
import { AppRoutes } from './AppRoutes'
import { ClientesPage } from './pages/clientes/ClientesPage'
import { renderWithProviders } from './test/utils'

vi.mock('./api/ordens', () => ({
  listarOrdens: vi.fn().mockResolvedValue({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  }),
  listarOrdensDoCliente: vi.fn(),
  buscarOrdem: vi.fn(),
  criarOrdem: vi.fn(),
  adicionarServico: vi.fn(),
  calcularOrcamento: vi.fn(),
  confirmarOrdem: vi.fn(),
  fecharOrdem: vi.fn(),
  alterarStatus: vi.fn(),
  cancelarOrdem: vi.fn(),
  removerServico: vi.fn(),
  atualizarServico: vi.fn(),
  abrirComprovante: vi.fn().mockResolvedValue(undefined),
  listarHistorico: vi.fn(),
}))

vi.mock('./api/pecas', () => ({
  listarBaixoEstoque: vi.fn().mockResolvedValue({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  }),
  listarPecas: vi.fn(),
  criarPeca: vi.fn(),
  buscarPeca: vi.fn(),
  registrarEntrada: vi.fn(),
}))

vi.mock('./api/clientes', () => ({
  listarClientes: vi.fn(),
  buscarCliente: vi.fn(),
  criarCliente: vi.fn(),
  listarMotosDoCliente: vi.fn(),
  vincularMoto: vi.fn(),
  desvincularMoto: vi.fn(),
}))

vi.mock('./api/motos', () => ({
  listarMotos: vi.fn(),
  filtrarMotos: vi.fn(),
  buscarMoto: vi.fn(),
  criarMoto: vi.fn(),
}))

describe('acessibilidade automatizada (axe)', () => {
  it('painel inicial não tem violações', async () => {
    const { container } = renderWithProviders(<AppRoutes />, { route: '/painel' })

    await screen.findByRole('heading', { level: 1, name: /painel da oficina/i })
    const resultado = await axe(container)
    expect(resultado.violations).toEqual([])
  })

  it('landing page não tem violações', async () => {
    const { container } = renderWithProviders(<AppRoutes />)

    await screen.findByRole('heading', { level: 1, name: /a oficina inteira/i })
    const resultado = await axe(container)
    expect(resultado.violations).toEqual([])
  })

  it('página 404 não tem violações', async () => {
    const { container } = renderWithProviders(<AppRoutes />, {
      route: '/pagina-inexistente',
    })

    await screen.findByRole('heading', { level: 1, name: /página não encontrada/i })
    const resultado = await axe(container)
    expect(resultado.violations).toEqual([])
  })

  it('tela de clientes (estado inicial) não tem violações', async () => {
    const { container } = renderWithProviders(<ClientesPage />)

    await screen.findByRole('heading', { level: 1, name: 'Clientes' })
    const resultado = await axe(container)
    expect(resultado.violations).toEqual([])
  })
})

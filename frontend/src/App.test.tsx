import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AppRoutes } from './AppRoutes'
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

describe('estrutura acessível', () => {
  it('oferece skip link, navegação principal e landmark de conteúdo', () => {
    renderWithProviders(<AppRoutes />)

    expect(screen.getByRole('link', { name: /pular para o conteúdo/i })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: /principal/i })).toBeInTheDocument()
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { level: 1, name: /a oficina inteira/i }),
    ).toBeInTheDocument()
  })

  it('painel em /painel mantém a navegação do app', async () => {
    renderWithProviders(<AppRoutes />, { route: '/painel' })

    expect(
      await screen.findByRole('heading', { level: 1, name: /painel da oficina/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: /principal/i })).toBeInTheDocument()
  })

  it('mostra página 404 com caminho de volta', () => {
    renderWithProviders(<AppRoutes />, { route: '/endereco-inexistente' })

    expect(
      screen.getByRole('heading', { level: 1, name: /página não encontrada/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /voltar ao painel/i })).toHaveAttribute('href', '/painel')
  })
})

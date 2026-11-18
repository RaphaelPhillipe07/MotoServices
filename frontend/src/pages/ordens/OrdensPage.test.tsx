import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as motosApi from '../../api/motos'
import * as ordensApi from '../../api/ordens'
import type { OrdemDeServico, Page } from '../../api/types'
import { renderWithProviders } from '../../test/utils'
import { OrdensPage } from './OrdensPage'

vi.mock('../../api/ordens', () => ({
  listarOrdens: vi.fn(),
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

vi.mock('../../api/clientes', () => ({
  listarClientes: vi.fn(),
  buscarCliente: vi.fn(),
  criarCliente: vi.fn(),
  listarMotosDoCliente: vi.fn(),
  vincularMoto: vi.fn(),
  desvincularMoto: vi.fn(),
}))

vi.mock('../../api/motos', () => ({
  listarMotos: vi.fn(),
  filtrarMotos: vi.fn(),
  buscarMoto: vi.fn(),
  criarMoto: vi.fn(),
}))

const ordem: OrdemDeServico = {
  id: 1,
  clienteId: 1,
  clienteNome: 'Maria Silva',
  motoId: 2,
  motoMarca: 'Honda',
  motoModelo: 'CG 160',
  motoCilindradas: 160,
  status: 'EM_ANDAMENTO',
  dataAbertura: '2026-11-05T10:00:00',
  dataFechamento: null,
  formaPagamento: null,
  totalPecas: null,
  totalMaoDeObra: null,
  descontoAplicado: null,
  totalFinal: null,
  servicos: [],
}

function pagina<T>(content: T[]): Page<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    first: true,
    last: true,
  }
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(motosApi.listarMotos).mockResolvedValue({
    ...pagina([]),
    size: 100,
  })
})

describe('OrdensPage', () => {
  it('lista as ordens com status e cliente', async () => {
    vi.mocked(ordensApi.listarOrdens).mockResolvedValue(pagina([ordem]))

    renderWithProviders(<OrdensPage />)

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Ordens de serviço' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Maria Silva')).toBeInTheDocument()
    expect(
      within(screen.getByRole('table')).getByText('Em andamento'),
    ).toBeInTheDocument()
    const linkAbrir = screen.getByText('Abrir').closest('a')
    expect(linkAbrir).toHaveAttribute('href', '/ordens-servico/1')
    expect(ordensApi.listarOrdens).toHaveBeenCalledWith(
      { clienteId: undefined, status: undefined },
      0,
      20,
    )
  })

  it('filtra por status e envia o parâmetro', async () => {
    vi.mocked(ordensApi.listarOrdens).mockResolvedValue(pagina([ordem]))
    const user = userEvent.setup()
    renderWithProviders(<OrdensPage />)

    await screen.findByText('Maria Silva')
    await user.selectOptions(screen.getByLabelText('Status'), 'ABERTA')

    await waitFor(() =>
      expect(ordensApi.listarOrdens).toHaveBeenLastCalledWith(
        { clienteId: undefined, status: 'ABERTA' },
        0,
        20,
      ),
    )
  })

  it('mostra estado vazio quando não há ordens', async () => {
    vi.mocked(ordensApi.listarOrdens).mockResolvedValue(pagina([]))

    renderWithProviders(<OrdensPage />)

    expect(await screen.findByText('Nenhuma OS aberta')).toBeInTheDocument()
  })
})

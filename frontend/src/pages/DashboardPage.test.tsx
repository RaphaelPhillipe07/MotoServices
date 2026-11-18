import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as ordensApi from '../api/ordens'
import * as pecasApi from '../api/pecas'
import type { OrdemDeServico, Page, Peca } from '../api/types'
import { renderWithProviders } from '../test/utils'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/ordens', () => ({
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

vi.mock('../api/pecas', () => ({
  listarBaixoEstoque: vi.fn(),
  listarPecas: vi.fn(),
  criarPeca: vi.fn(),
  buscarPeca: vi.fn(),
  registrarEntrada: vi.fn(),
}))

function ordem(overrides: Partial<OrdemDeServico> = {}): OrdemDeServico {
  return {
    id: 12,
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
    ...overrides,
  }
}

const recémChegada = ordem({
  id: 31,
  clienteNome: 'João Dias',
  motoMarca: 'Yamaha',
  motoModelo: 'Factor',
  status: 'ABERTA',
  dataAbertura: '2026-11-11T09:00:00',
})

const esperandoCliente = ordem({
  id: 22,
  clienteNome: 'Carla Nunes',
  motoMarca: 'Honda',
  motoModelo: 'Biz',
  status: 'AGUARDANDO_CLIENTE',
  dataAbertura: '2026-11-04T08:00:00',
})

const pecaCritica: Peca = {
  id: 2,
  codigo: 'P-002',
  descricao: 'Filtro de óleo',
  valor: 45.5,
  quantidadeEstoque: 1,
  estoqueMinimo: 4,
}

function pagina<T>(content: T[], total: number, size: number): Page<T> {
  return {
    content,
    page: 0,
    size,
    totalElements: total,
    totalPages: total === 0 ? 0 : Math.ceil(total / size),
    first: true,
    last: true,
  }
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(ordensApi.listarOrdens).mockImplementation((filtros, _page, size = 10) => {
    if (filtros?.status === 'EM_ANDAMENTO') return Promise.resolve(pagina([ordem()], 2, size))
    if (filtros?.status === 'AGUARDANDO_CLIENTE')
      return Promise.resolve(pagina([esperandoCliente], 1, size))
    if (filtros?.status === 'ABERTA') return Promise.resolve(pagina([recémChegada], 3, size))
    if (filtros?.status === 'CONCLUIDA') return Promise.resolve(pagina([], 1, size))
    return Promise.resolve(pagina([], 0, size))
  })
  vi.mocked(pecasApi.listarBaixoEstoque).mockResolvedValue(pagina([pecaCritica], 1, 4))
})

describe('DashboardPage', () => {
  it('mostra o pátio, o indicador consolidado e o estoque crítico', async () => {
    renderWithProviders(<DashboardPage />)

    expect(
      screen.getByRole('heading', { level: 1, name: 'Painel da oficina' }),
    ).toBeInTheDocument()

    const patio = await screen.findByRole('link', { name: /na oficina/i })
    await waitFor(() => expect(patio).toHaveTextContent('6'))
    expect(patio).toHaveAttribute('href', '/ordens-servico')
    expect(patio).toHaveTextContent(/2 em execução · 1 esperando o cliente · 3 aguardando início/)

    expect(await screen.findByText('Maria Silva')).toBeInTheDocument()
    expect(screen.getByText(/Honda CG 160/)).toBeInTheDocument()

    expect(await screen.findByText('Filtro de óleo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /repor filtro de óleo/i })).toHaveAttribute(
      'href',
      '/estoque',
    )
  })

  it('ordena as motos da que espera há mais tempo para a mais recente', async () => {
    renderWithProviders(<DashboardPage />)

    const itens = await screen.findAllByRole('listitem')
    const motos = itens.map((item) => item.textContent ?? '')
    const posicaoBiz = motos.findIndex((texto) => texto.includes('Biz'))
    const posicaoFactor = motos.findIndex((texto) => texto.includes('Factor'))

    expect(posicaoBiz).toBeGreaterThanOrEqual(0)
    expect(posicaoFactor).toBeGreaterThan(posicaoBiz)
  })

  it('não lista ordens fechadas no pátio', async () => {
    renderWithProviders(<DashboardPage />)

    await screen.findByText('Maria Silva')

    expect(ordensApi.listarOrdens).not.toHaveBeenCalledWith(
      expect.objectContaining({ status: 'FECHADA' }),
      expect.anything(),
      expect.anything(),
    )
  })

  it('mantém um único atalho de abertura de OS no cabeçalho', async () => {
    renderWithProviders(<DashboardPage />)

    const abertura = await screen.findAllByRole('link', { name: /abrir os/i })
    expect(abertura).toHaveLength(1)
    expect(abertura[0]).toHaveAttribute('href', '/ordens-servico?nova=1')
    expect(screen.getAllByRole('link', { name: /ver todas/i })[0]).toHaveAttribute(
      'href',
      '/ordens-servico',
    )
  })

  it('mostra estoque saudável quando não há peças críticas', async () => {
    vi.mocked(pecasApi.listarBaixoEstoque).mockResolvedValue(pagina([], 0, 4))

    renderWithProviders(<DashboardPage />)

    expect(await screen.findByText('Estoque saudável')).toBeInTheDocument()
  })

  it('mostra o pátio vazio com ação de abrir OS', async () => {
    vi.mocked(ordensApi.listarOrdens).mockResolvedValue(pagina([], 0, 20))

    renderWithProviders(<DashboardPage />)

    const vazio = await screen.findByText('Pátio vazio')
    expect(vazio).toBeInTheDocument()

    const regiao = vazio.closest('div') as HTMLElement
    expect(within(regiao).getByRole('link', { name: /abrir os/i })).toHaveAttribute(
      'href',
      '/ordens-servico?nova=1',
    )
  })
})

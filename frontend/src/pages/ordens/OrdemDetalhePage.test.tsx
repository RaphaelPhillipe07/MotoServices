import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as ordensApi from '../../api/ordens'
import type { HistoricoStatus, OrdemDeServico, Page } from '../../api/types'
import { renderWithProviders } from '../../test/utils'
import { OrdemDetalhePage } from './OrdemDetalhePage'

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

const ordem: OrdemDeServico = {
  id: 1,
  clienteId: 1,
  clienteNome: 'Maria Silva',
  motoId: 2,
  motoMarca: 'Honda',
  motoModelo: 'CG 160',
  motoCilindradas: 160,
  status: 'ABERTA',
  dataAbertura: '2026-11-05T10:00:00',
  dataFechamento: null,
  formaPagamento: null,
  totalPecas: null,
  totalMaoDeObra: null,
  descontoAplicado: null,
  totalFinal: null,
  servicos: [
    {
      id: 10,
      tipo: 'REVISAO',
      descricao: 'Revisão de 10.000 km',
      tempoEstimado: 30,
      valorBaseMaoDeObra: 100,
      valorFinal: 150,
      pecaId: null,
      pecaFornecidaPelaLoja: null,
    },
  ],
}

function pagina<T>(content: T[], size = 20): Page<T> {
  return {
    content,
    page: 0,
    size,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    first: true,
    last: true,
  }
}

const historico: HistoricoStatus[] = [
  {
    id: 1,
    osId: 1,
    statusAnterior: null,
    statusNovo: 'ABERTA',
    dataHora: '2026-11-05T10:00:00',
  },
]

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(ordensApi.buscarOrdem).mockResolvedValue(ordem)
  vi.mocked(ordensApi.calcularOrcamento).mockResolvedValue({
    totalPecas: 0,
    totalMaoDeObra: 150,
    descontoAplicado: 0,
    totalFinal: 150,
  })
  vi.mocked(ordensApi.listarHistorico).mockResolvedValue(pagina(historico))
})

describe('OrdemDetalhePage', () => {
  function renderDetalhe() {
    return renderWithProviders(
      <Routes>
        <Route path="/ordens-servico/:osId" element={<OrdemDetalhePage />} />
      </Routes>,
      { route: '/ordens-servico/1' },
    )
  }

  it('mostra dados, serviço, orçamento e histórico', async () => {
    renderDetalhe()

    expect(
      await screen.findByRole('heading', { level: 1, name: 'OS #1' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Revisão de 10.000 km')).toBeInTheDocument()
    expect(screen.getAllByText(/R\$\s*150,00/).length).toBeGreaterThan(0)
    expect(screen.getByText(/OS aberta como Aberta/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /confirmar os/i })).toBeInTheDocument()
  })

  it('confirma a OS e exibe feedback', async () => {
    vi.mocked(ordensApi.confirmarOrdem).mockResolvedValue({
      ...ordem,
      status: 'EM_ANDAMENTO',
    })
    const user = userEvent.setup()
    renderDetalhe()

    await screen.findByRole('heading', { level: 1, name: 'OS #1' })
    await user.click(screen.getByRole('button', { name: /confirmar os/i }))

    await waitFor(() => expect(ordensApi.confirmarOrdem).toHaveBeenCalledWith(1))
    expect(await screen.findByText('OS confirmada e estoque baixado.')).toBeInTheDocument()
  })

  it('adiciona serviço pelo diálogo', async () => {
    vi.mocked(ordensApi.adicionarServico).mockResolvedValue(ordem)
    const user = userEvent.setup()
    renderDetalhe()

    await screen.findByRole('heading', { level: 1, name: 'OS #1' })
    await user.click(screen.getByRole('button', { name: /adicionar serviço/i }))
    const dialogo = await screen.findByRole('dialog')

    await user.click(
      within(dialogo).getByRole('button', { name: /adicionar serviço/i }),
    )
    expect(await screen.findByText('Informe a descrição.')).toBeInTheDocument()

    await user.type(within(dialogo).getByLabelText(/^Descrição/), 'Troca de óleo')
    await user.type(within(dialogo).getByLabelText(/^Mão de obra/), '80')
    await user.click(
      within(dialogo).getByRole('button', { name: /adicionar serviço/i }),
    )

    await waitFor(() =>
      expect(ordensApi.adicionarServico).toHaveBeenCalledWith(1, {
        tipo: 'REVISAO',
        descricao: 'Troca de óleo',
        valorBaseMaoDeObra: 80,
        tempoEstimado: null,
        checklistGeral: false,
        pecaFornecidaPelaLoja: null,
        pecaId: null,
      }),
    )
    expect(await screen.findByText('Serviço adicionado ao orçamento.')).toBeInTheDocument()
  })
})

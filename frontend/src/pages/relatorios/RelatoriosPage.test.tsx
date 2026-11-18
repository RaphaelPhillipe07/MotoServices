import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as relatoriosApi from '../../api/relatorios'
import type { ClienteRanking, Faturamento, ServicoRanking } from '../../api/types'
import { renderWithProviders } from '../../test/utils'
import { RelatoriosPage } from './RelatoriosPage'

vi.mock('../../api/relatorios', () => ({
  faturamento: vi.fn(),
  servicosMaisRealizados: vi.fn(),
  clientesMaisAtendidos: vi.fn(),
  baixarExportacao: vi.fn(),
}))

const faturamentoMock: Faturamento = {
  inicio: '2026-11-01',
  fim: '2026-11-30',
  totalPecas: 120,
  totalMaoDeObra: 230,
  descontoAplicado: 0,
  totalFinal: 350,
  ticketMedio: 350,
  quantidadeOs: 1,
}

const servicosMock: ServicoRanking[] = [
  {
    descricao: 'Revisão de 10.000 km',
    tipo: 'REVISAO',
    quantidade: 1,
    totalBaseMaoDeObra: 150,
  },
]

const clientesMock: ClienteRanking[] = [
  { clienteId: 1, clienteNome: 'Maria Silva', quantidadeOs: 1, totalFinal: 350 },
]

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(relatoriosApi.faturamento).mockResolvedValue(faturamentoMock)
  vi.mocked(relatoriosApi.servicosMaisRealizados).mockResolvedValue(servicosMock)
  vi.mocked(relatoriosApi.clientesMaisAtendidos).mockResolvedValue(clientesMock)
  vi.mocked(relatoriosApi.baixarExportacao).mockResolvedValue(undefined)
})

describe('RelatoriosPage', () => {
  it('mostra os indicadores e os rankings do período', async () => {
    renderWithProviders(<RelatoriosPage />)

    const indicadores = await screen.findByRole('region', { name: /indicadores do período/i })
    expect(within(indicadores).getAllByText('R$ 350,00').length).toBeGreaterThanOrEqual(1)
    expect(within(indicadores).getByText(/1 OS fechada/i)).toBeInTheDocument()
    expect(await screen.findByText('Revisão de 10.000 km')).toBeInTheDocument()
    expect(await screen.findByText('Maria Silva')).toBeInTheDocument()
  })

  it('exporta o CSV com o período selecionado', async () => {
    const user = userEvent.setup()
    renderWithProviders(<RelatoriosPage />)

    await screen.findByRole('region', { name: /indicadores do período/i })
    await user.click(screen.getByRole('button', { name: /exportar csv/i }))

    expect(relatoriosApi.baixarExportacao).toHaveBeenCalledTimes(1)
    const [inicio, fim] = vi.mocked(relatoriosApi.baixarExportacao).mock.calls[0]
    expect(inicio).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(fim).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('bloqueia a exportação quando o período está invertido', async () => {
    const user = userEvent.setup()
    renderWithProviders(<RelatoriosPage />)

    const inicio = screen.getByLabelText('Início')
    const fim = screen.getByLabelText('Fim')
    await user.clear(inicio)
    await user.type(inicio, '2026-12-31')
    await user.clear(fim)
    await user.type(fim, '2026-01-01')

    expect(screen.getByRole('button', { name: /exportar csv/i })).toBeDisabled()
    expect(screen.getByText(/data de início não pode ser posterior/i)).toBeInTheDocument()
  })
})

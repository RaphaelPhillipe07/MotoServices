import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as pecasApi from '../../api/pecas'
import type { Page, Peca } from '../../api/types'
import { renderWithProviders } from '../../test/utils'
import { EstoquePage } from './EstoquePage'

vi.mock('../../api/pecas', () => ({
  listarPecas: vi.fn(),
  listarBaixoEstoque: vi.fn(),
  criarPeca: vi.fn(),
  buscarPeca: vi.fn(),
  registrarEntrada: vi.fn(),
}))

const pastilha: Peca = {
  id: 1,
  codigo: 'P-001',
  descricao: 'Pastilha de freio',
  valor: 80,
  quantidadeEstoque: 5,
  estoqueMinimo: 2,
}

const filtro: Peca = {
  id: 2,
  codigo: 'P-002',
  descricao: 'Filtro de óleo',
  valor: 45.9,
  quantidadeEstoque: 1,
  estoqueMinimo: 3,
}

function pagina<T>(content: T[]): Page<T> {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    first: true,
    last: true,
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EstoquePage', () => {
  it('lista peças e alterna para o alerta de ruptura', async () => {
    vi.mocked(pecasApi.listarPecas).mockResolvedValue(pagina([pastilha]))
    vi.mocked(pecasApi.listarBaixoEstoque).mockResolvedValue(pagina([filtro]))
    const user = userEvent.setup()
    renderWithProviders(<EstoquePage />)

    expect(await screen.findByText('Pastilha de freio')).toBeInTheDocument()
    expect(screen.getByText('OK')).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'Estoque baixo' }))

    expect(await screen.findByText('Filtro de óleo')).toBeInTheDocument()
    expect(screen.getByText('Abaixo do mínimo')).toBeInTheDocument()
    expect(pecasApi.listarBaixoEstoque).toHaveBeenCalled()
  })

  it('registra entrada de estoque pela linha da peça', async () => {
    vi.mocked(pecasApi.listarPecas).mockResolvedValue(pagina([pastilha]))
    vi.mocked(pecasApi.listarBaixoEstoque).mockResolvedValue(pagina([]))
    vi.mocked(pecasApi.registrarEntrada).mockResolvedValue({
      ...pastilha,
      quantidadeEstoque: 8,
    })
    const user = userEvent.setup()
    renderWithProviders(<EstoquePage />)

    expect(await screen.findByText('Pastilha de freio')).toBeInTheDocument()

    const botaoEntrada = screen.getByText('Entrada').closest('button')
    expect(botaoEntrada).not.toBeNull()
    await user.click(botaoEntrada!)
    expect(await screen.findByRole('dialog')).toBeInTheDocument()

    await user.type(screen.getByLabelText(/^Quantidade recebida/), '3')
    await user.click(screen.getByRole('button', { name: /registrar entrada/i }))

    await waitFor(() =>
      expect(pecasApi.registrarEntrada).toHaveBeenCalledWith(1, { quantidade: 3 }),
    )
    expect(await screen.findByText(/entrada de 3 registrada para p-001/i)).toBeInTheDocument()
  })
})

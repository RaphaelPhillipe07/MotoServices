import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as motosApi from '../../api/motos'
import type { Moto, Page } from '../../api/types'
import { renderWithProviders } from '../../test/utils'
import { MotosPage } from './MotosPage'

vi.mock('../../api/motos', () => ({
  listarMotos: vi.fn(),
  filtrarMotos: vi.fn(),
  buscarMoto: vi.fn(),
  criarMoto: vi.fn(),
}))

const moto: Moto = { id: 2, marca: 'Honda', modelo: 'CG 160', cilindradas: 160 }

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

describe('MotosPage', () => {
  it('lista o catálogo com a faixa completa', async () => {
    vi.mocked(motosApi.filtrarMotos).mockResolvedValue(pagina([moto]))

    renderWithProviders(<MotosPage />)

    expect(await screen.findByText('CG 160')).toBeInTheDocument()
    expect(screen.getByText('160 cc')).toBeInTheDocument()
    expect(motosApi.filtrarMotos).toHaveBeenCalledWith(1, 2000, 0, 10)
  })

  it('rejeita faixa de cilindradas invertida', async () => {
    vi.mocked(motosApi.filtrarMotos).mockResolvedValue(pagina([moto]))
    const user = userEvent.setup()
    renderWithProviders(<MotosPage />)

    const minimo = screen.getByLabelText('Cilindradas mínimas')
    const maximo = screen.getByLabelText('Cilindradas máximas')
    await user.clear(minimo)
    await user.type(minimo, '500')
    await user.clear(maximo)
    await user.type(maximo, '100')
    await user.click(screen.getByRole('button', { name: 'Filtrar' }))

    expect(
      await screen.findByText('A cilindrada máxima deve ser maior ou igual à mínima.'),
    ).toBeInTheDocument()
    expect(motosApi.filtrarMotos).toHaveBeenCalledTimes(1)
  })

  it('valida e envia o cadastro de moto', async () => {
    vi.mocked(motosApi.filtrarMotos).mockResolvedValue(pagina([moto]))
    vi.mocked(motosApi.criarMoto).mockResolvedValue({
      id: 3,
      marca: 'Yamaha',
      modelo: 'XTZ 250',
      cilindradas: 250,
    })
    const user = userEvent.setup()
    renderWithProviders(<MotosPage />)

    await user.click(screen.getByRole('button', { name: /nova moto/i }))
    const dialogo = await screen.findByRole('dialog')
    expect(dialogo).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /salvar moto/i }))
    expect(await screen.findByText('Informe a marca.')).toBeInTheDocument()
    expect(motosApi.criarMoto).not.toHaveBeenCalled()

    await user.type(within(dialogo).getByLabelText(/^Marca/), 'Yamaha')
    await user.type(within(dialogo).getByLabelText(/^Modelo/), 'XTZ 250')
    await user.type(within(dialogo).getByLabelText(/^Cilindradas/), '250')
    await user.click(screen.getByRole('button', { name: /salvar moto/i }))

    await waitFor(() => expect(motosApi.criarMoto).toHaveBeenCalledTimes(1))
    expect(motosApi.criarMoto).toHaveBeenCalledWith({
      marca: 'Yamaha',
      modelo: 'XTZ 250',
      cilindradas: 250,
    })
    expect(await screen.findByText(/moto yamaha xtz 250 cadastrada/i)).toBeInTheDocument()
  })
})

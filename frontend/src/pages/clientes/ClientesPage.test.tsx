import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as clientesApi from '../../api/clientes'
import { ApiError } from '../../api/http'
import * as motosApi from '../../api/motos'
import type { Cliente, Moto, Page } from '../../api/types'
import { renderWithProviders } from '../../test/utils'
import { ClienteDetalhePage } from './ClienteDetalhePage'
import { ClientesPage } from './ClientesPage'

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

const cliente: Cliente = {
  id: 1,
  nome: 'Maria Silva',
  cpfCnpj: '52998224725',
  contato: '11999999999',
  proximaRevisao: '2027-05-10',
}

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

describe('ClientesPage', () => {
  it('inicia com convite de busca e cadastro', () => {
    renderWithProviders(<ClientesPage />)

    expect(screen.getByRole('heading', { level: 1, name: 'Clientes' })).toBeInTheDocument()
    expect(screen.getByText('Busque um cliente')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /novo cliente/i })).toBeInTheDocument()
  })

  it('valida a busca vazia sem chamar a API', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ClientesPage />)

    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText(/informe um nome, cpf ou cnpj/i)).toBeInTheDocument()
    expect(clientesApi.listarClientes).not.toHaveBeenCalled()
  })

  it('lista os clientes encontrados com link de detalhe', async () => {
    vi.mocked(clientesApi.listarClientes).mockResolvedValue(pagina([cliente]))
    const user = userEvent.setup()
    renderWithProviders(<ClientesPage />)

    await user.type(screen.getByLabelText('Buscar cliente'), 'Maria')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    const linkDetalhes = await screen.findByText('Detalhes')
    const ancora = linkDetalhes.closest('a')
    expect(ancora).toHaveAttribute('href', '/clientes/1')
    expect(ancora).toHaveAccessibleName(/Maria Silva/)
    expect(clientesApi.listarClientes).toHaveBeenCalledWith('Maria', 0, 10)
  })

  it('mostra estado vazio quando não há resultados', async () => {
    vi.mocked(clientesApi.listarClientes).mockResolvedValue(pagina([]))
    const user = userEvent.setup()
    renderWithProviders(<ClientesPage />)

    await user.type(screen.getByLabelText('Buscar cliente'), 'Inexistente')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Nenhum cliente encontrado')).toBeInTheDocument()
  })

  it('mostra erro recuperável quando a API falha', async () => {
    vi.mocked(clientesApi.listarClientes).mockRejectedValue(
      new ApiError('Falha ao consultar clientes.', 500),
    )
    const user = userEvent.setup()
    renderWithProviders(<ClientesPage />)

    await user.type(screen.getByLabelText('Buscar cliente'), 'Maria')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao consultar clientes.')
    expect(screen.getByRole('button', { name: /tentar novamente/i })).toBeInTheDocument()
  })

  it('valida e envia o cadastro de cliente', async () => {
    vi.mocked(clientesApi.listarClientes).mockResolvedValue(pagina([cliente]))
    vi.mocked(clientesApi.criarCliente).mockResolvedValue(cliente)
    const user = userEvent.setup()
    renderWithProviders(<ClientesPage />)

    await user.click(screen.getByRole('button', { name: /novo cliente/i }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /salvar cliente/i }))
    expect(await screen.findByText('Informe ao menos 3 caracteres.')).toBeInTheDocument()
    expect(clientesApi.criarCliente).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText(/^Nome/), 'Maria Silva')
    await user.type(screen.getByLabelText(/^CPF ou CNPJ/), '52998224725')
    await user.type(screen.getByLabelText(/^Contato/), '11999999999')
    await user.click(screen.getByRole('button', { name: /salvar cliente/i }))

    await waitFor(() => expect(clientesApi.criarCliente).toHaveBeenCalledTimes(1))
    expect(clientesApi.criarCliente).toHaveBeenCalledWith({
      nome: 'Maria Silva',
      cpfCnpj: '52998224725',
      contato: '11999999999',
      proximaRevisao: null,
    })
    expect(await screen.findByText(/cliente maria silva cadastrado/i)).toBeInTheDocument()
  })
})

describe('ClienteDetalhePage', () => {
  it('mostra dados do cliente e motos vinculadas', async () => {
    vi.mocked(clientesApi.buscarCliente).mockResolvedValue(cliente)
    vi.mocked(clientesApi.listarMotosDoCliente).mockResolvedValue(pagina([moto]))
    vi.mocked(motosApi.listarMotos).mockResolvedValue(pagina([moto]))

    renderWithProviders(
      <Routes>
        <Route path="/clientes/:clienteId" element={<ClienteDetalhePage />} />
      </Routes>,
      { route: '/clientes/1' },
    )

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Maria Silva' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('CG 160')).toBeInTheDocument()
    expect(screen.getAllByText('529.982.247-25').length).toBeGreaterThan(0)
  })
})

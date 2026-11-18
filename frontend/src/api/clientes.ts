import { apiRequest, buildQuery } from './http'
import type { Cliente, ClienteRequest, Moto, Page } from './types'

export function listarClientes(termo: string, page = 0, size = 10) {
  return apiRequest<Page<Cliente>>(
    `/clientes${buildQuery({ nomeOuCpf: termo, page, size, sort: 'nome' })}`,
  )
}

export function buscarCliente(clienteId: number) {
  return apiRequest<Cliente>(`/clientes/${clienteId}`)
}

export function criarCliente(data: ClienteRequest) {
  return apiRequest<Cliente>('/clientes', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function listarMotosDoCliente(clienteId: number, page = 0, size = 20) {
  return apiRequest<Page<Moto>>(
    `/clientes/${clienteId}/motos${buildQuery({ page, size })}`,
  )
}

export function vincularMoto(clienteId: number, motoId: number) {
  return apiRequest<void>(`/clientes/${clienteId}/motos/${motoId}`, { method: 'POST' })
}

export function desvincularMoto(clienteId: number, motoId: number) {
  return apiRequest<void>(`/clientes/${clienteId}/motos/${motoId}`, { method: 'DELETE' })
}

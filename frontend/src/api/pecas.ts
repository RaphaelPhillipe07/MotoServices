import { apiRequest, buildQuery } from './http'
import type { EntradaEstoqueRequest, Page, Peca, PecaRequest } from './types'

export function listarPecas(busca = '', page = 0, size = 10) {
  return apiRequest<Page<Peca>>(
    `/pecas${buildQuery({ busca: busca.trim() || undefined, page, size, sort: 'codigo' })}`,
  )
}

export function buscarPeca(pecaId: number) {
  return apiRequest<Peca>(`/pecas/${pecaId}`)
}

export function criarPeca(data: PecaRequest) {
  return apiRequest<Peca>('/pecas', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function registrarEntrada(pecaId: number, data: EntradaEstoqueRequest) {
  return apiRequest<Peca>(`/pecas/${pecaId}/entrada`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function listarBaixoEstoque(page = 0, size = 10) {
  return apiRequest<Page<Peca>>(
    `/estoque/baixo${buildQuery({ page, size, sort: 'codigo' })}`,
  )
}

import { apiRequest, buildQuery } from './http'
import type { Moto, MotoRequest, Page } from './types'

export function filtrarMotos(minimo: number, maximo: number, page = 0, size = 10) {
  return apiRequest<Page<Moto>>(
    `/motos/filtrar${buildQuery({ minimo, maximo, page, size, sort: 'cilindradas' })}`,
  )
}

/** Lista o catálogo inteiro usando a faixa máxima suportada pela API. */
export function listarMotos(page = 0, size = 10) {
  return filtrarMotos(1, 2000, page, size)
}

export function buscarMoto(motoId: number) {
  return apiRequest<Moto>(`/motos/${motoId}`)
}

export function criarMoto(data: MotoRequest) {
  return apiRequest<Moto>('/motos', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

import { apiRequest, baixarArquivo, buildQuery } from './http'
import type { ClienteRanking, Faturamento, ServicoRanking } from './types'

export function faturamento(inicio: string, fim: string) {
  return apiRequest<Faturamento>(
    `/relatorios/faturamento${buildQuery({ inicio, fim })}`,
  )
}

export function servicosMaisRealizados(inicio: string, fim: string, limite = 5) {
  return apiRequest<ServicoRanking[]>(
    `/relatorios/servicos-mais-realizados${buildQuery({ inicio, fim, limite })}`,
  )
}

export function clientesMaisAtendidos(inicio: string, fim: string, limite = 5) {
  return apiRequest<ClienteRanking[]>(
    `/relatorios/clientes-mais-atendidos${buildQuery({ inicio, fim, limite })}`,
  )
}

export function baixarExportacao(inicio: string, fim: string) {
  return baixarArquivo(
    `/relatorios/ordens/exportar${buildQuery({ inicio, fim })}`,
    `ordens-${inicio}-a-${fim}.csv`,
  )
}

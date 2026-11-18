import { abrirArquivo, apiRequest, buildQuery } from './http'
import type {
  FormaPagamento,
  HistoricoStatus,
  Orcamento,
  OrdemDeServico,
  OrdemDeServicoRequest,
  Page,
  ServicoRequest,
  StatusOS,
} from './types'

export function listarOrdens(
  filtros: { clienteId?: number; motoId?: number; status?: StatusOS } = {},
  page = 0,
  size = 10,
) {
  return apiRequest<Page<OrdemDeServico>>(
    `/ordens-servico${buildQuery({
      clienteId: filtros.clienteId,
      motoId: filtros.motoId,
      status: filtros.status,
      page,
      size,
      sort: 'dataAbertura,desc',
    })}`,
  )
}

export function listarOrdensDoCliente(clienteId: number, page = 0, size = 10) {
  return listarOrdens({ clienteId }, page, size)
}

export function buscarOrdem(osId: number) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}`)
}

export function criarOrdem(data: OrdemDeServicoRequest) {
  return apiRequest<OrdemDeServico>('/ordens-servico', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function adicionarServico(osId: number, data: ServicoRequest) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}/servicos`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function calcularOrcamento(osId: number) {
  return apiRequest<Orcamento>(`/ordens-servico/${osId}/orcamento`)
}

export function confirmarOrdem(osId: number) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}/confirmar`, { method: 'POST' })
}

export function fecharOrdem(osId: number, formaPagamento?: FormaPagamento) {
  return apiRequest<Orcamento>(`/ordens-servico/${osId}/fechar`, {
    method: 'POST',
    body: JSON.stringify(formaPagamento ? { formaPagamento } : {}),
  })
}

export function alterarStatus(osId: number, status: StatusOS) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function cancelarOrdem(osId: number) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}/cancelar`, { method: 'POST' })
}

export function removerServico(osId: number, servicoId: number) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}/servicos/${servicoId}`, {
    method: 'DELETE',
  })
}

export function atualizarServico(osId: number, servicoId: number, data: ServicoRequest) {
  return apiRequest<OrdemDeServico>(`/ordens-servico/${osId}/servicos/${servicoId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export function abrirComprovante(osId: number) {
  return abrirArquivo(`/ordens-servico/${osId}/comprovante`)
}

export function listarHistorico(osId: number, page = 0, size = 10) {
  return apiRequest<Page<HistoricoStatus>>(
    `/ordens-servico/${osId}/historico${buildQuery({ page, size })}`,
  )
}

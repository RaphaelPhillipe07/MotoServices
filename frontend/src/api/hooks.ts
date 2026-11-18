import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as clientesApi from './clientes'
import * as motosApi from './motos'
import * as ordensApi from './ordens'
import * as pecasApi from './pecas'
import * as relatoriosApi from './relatorios'
import type {
  ClienteRequest,
  FormaPagamento,
  MotoRequest,
  OrdemDeServicoRequest,
  PecaRequest,
  ServicoRequest,
  StatusOS,
} from './types'

export const queryKeys = {
  clientes: (termo: string, page: number, size: number) =>
    ['clientes', termo, page, size] as const,
  cliente: (clienteId: number) => ['clientes', clienteId] as const,
  motosDoCliente: (clienteId: number, page: number) =>
    ['clientes', clienteId, 'motos', page] as const,
  motos: (page: number, size: number) => ['motos', page, size] as const,
  pecas: (busca: string, page: number) => ['pecas', busca, page] as const,
  baixoEstoque: (page: number) => ['pecas', 'baixo', page] as const,
  ordensDoCliente: (clienteId: number, page: number) =>
    ['ordens', clienteId, page] as const,
  ordens: (clienteId: number | undefined, status: StatusOS | undefined, page: number) =>
    ['ordens', 'filtro', clienteId ?? 'todos', status ?? 'todos', page] as const,
  ordem: (osId: number) => ['ordens', osId] as const,
  orcamento: (osId: number) => ['ordens', osId, 'orcamento'] as const,
  historico: (osId: number, page: number) => ['ordens', osId, 'historico', page] as const,
  faturamento: (inicio: string, fim: string) => ['relatorios', 'faturamento', inicio, fim] as const,
  servicosRanking: (inicio: string, fim: string) =>
    ['relatorios', 'servicos', inicio, fim] as const,
  clientesRanking: (inicio: string, fim: string) =>
    ['relatorios', 'clientes', inicio, fim] as const,
}

// ---------- clientes ----------

export function useBuscaClientes(termo: string, page = 0, size = 10) {
  return useQuery({
    queryKey: queryKeys.clientes(termo, page, size),
    queryFn: () => clientesApi.listarClientes(termo, page, size),
    enabled: termo.trim().length > 0,
    placeholderData: keepPreviousData,
  })
}

export function useCliente(clienteId: number) {
  return useQuery({
    queryKey: queryKeys.cliente(clienteId),
    queryFn: () => clientesApi.buscarCliente(clienteId),
    enabled: clienteId > 0,
  })
}

export function useCriarCliente() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: ClienteRequest) => clientesApi.criarCliente(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['clientes'] })
    },
  })
}

export function useMotosDoCliente(clienteId: number, page = 0, size = 20) {
  return useQuery({
    queryKey: queryKeys.motosDoCliente(clienteId, page),
    queryFn: () => clientesApi.listarMotosDoCliente(clienteId, page, size),
    enabled: clienteId > 0,
  })
}

export function useVincularMoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ clienteId, motoId }: { clienteId: number; motoId: number }) =>
      clientesApi.vincularMoto(clienteId, motoId),
    onSuccess: (_, { clienteId }) => {
      void queryClient.invalidateQueries({ queryKey: ['clientes', clienteId] })
    },
  })
}

export function useDesvincularMoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ clienteId, motoId }: { clienteId: number; motoId: number }) =>
      clientesApi.desvincularMoto(clienteId, motoId),
    onSuccess: (_, { clienteId }) => {
      void queryClient.invalidateQueries({ queryKey: ['clientes', clienteId] })
    },
  })
}

// ---------- motos ----------

export function useMotos(page = 0, size = 10) {
  return useQuery({
    queryKey: queryKeys.motos(page, size),
    queryFn: () => motosApi.listarMotos(page, size),
    placeholderData: keepPreviousData,
  })
}

export function useMotosFiltradas(minimo: number, maximo: number, page = 0, size = 10) {
  return useQuery({
    queryKey: ['motos', 'filtro', minimo, maximo, page, size] as const,
    queryFn: () => motosApi.filtrarMotos(minimo, maximo, page, size),
    placeholderData: keepPreviousData,
  })
}

export function useCriarMoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: MotoRequest) => motosApi.criarMoto(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['motos'] })
    },
  })
}

// ---------- peças e estoque ----------

export function usePecas(busca = '', page = 0, size = 10, enabled = true) {
  return useQuery({
    queryKey: queryKeys.pecas(busca, page),
    queryFn: () => pecasApi.listarPecas(busca, page, size),
    enabled,
    placeholderData: keepPreviousData,
  })
}

export function useBaixoEstoque(page = 0, size = 10, enabled = true) {
  return useQuery({
    queryKey: queryKeys.baixoEstoque(page),
    queryFn: () => pecasApi.listarBaixoEstoque(page, size),
    enabled,
    placeholderData: keepPreviousData,
  })
}

export function useCriarPeca() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: PecaRequest) => pecasApi.criarPeca(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pecas'] })
    },
  })
}

export function useEntradaEstoque() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ pecaId, quantidade }: { pecaId: number; quantidade: number }) =>
      pecasApi.registrarEntrada(pecaId, { quantidade }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pecas'] })
    },
  })
}

// ---------- ordens de serviço ----------

export function useOrdensDoCliente(clienteId: number, page = 0, size = 10) {
  return useOrdens(clienteId, undefined, page, size)
}

export function useOrdens(
  clienteId: number | undefined,
  status: StatusOS | undefined,
  page = 0,
  size = 10,
) {
  return useQuery({
    queryKey: queryKeys.ordens(clienteId, status, page),
    queryFn: () => ordensApi.listarOrdens({ clienteId, status }, page, size),
    placeholderData: keepPreviousData,
  })
}

export function useOrdem(osId: number) {
  return useQuery({
    queryKey: queryKeys.ordem(osId),
    queryFn: () => ordensApi.buscarOrdem(osId),
    enabled: osId > 0,
  })
}

export function useCriarOrdem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: OrdemDeServicoRequest) => ordensApi.criarOrdem(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
    },
  })
}

export function useAdicionarServico(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (servico: ServicoRequest) => ordensApi.adicionarServico(osId, servico),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
    },
  })
}

export function useOrcamento(osId: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.orcamento(osId),
    queryFn: () => ordensApi.calcularOrcamento(osId),
    enabled: enabled && osId > 0,
  })
}

export function useConfirmarOrdem(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => ordensApi.confirmarOrdem(osId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
    },
  })
}

export function useFecharOrdem(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (formaPagamento?: FormaPagamento) =>
      ordensApi.fecharOrdem(osId, formaPagamento),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
      void queryClient.invalidateQueries({ queryKey: ['clientes'] })
    },
  })
}

export function useCancelarOrdem(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => ordensApi.cancelarOrdem(osId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
      void queryClient.invalidateQueries({ queryKey: ['pecas'] })
    },
  })
}

export function useAlterarStatus(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (status: StatusOS) => ordensApi.alterarStatus(osId, status),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
    },
  })
}

export function useRemoverServico(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (servicoId: number) => ordensApi.removerServico(osId, servicoId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
      void queryClient.invalidateQueries({ queryKey: ['pecas'] })
    },
  })
}

export function useAtualizarServico(osId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ servicoId, data }: { servicoId: number; data: ServicoRequest }) =>
      ordensApi.atualizarServico(osId, servicoId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ordens'] })
      void queryClient.invalidateQueries({ queryKey: ['pecas'] })
    },
  })
}

export function useHistorico(osId: number, page = 0, size = 20) {
  return useQuery({
    queryKey: queryKeys.historico(osId, page),
    queryFn: () => ordensApi.listarHistorico(osId, page, size),
    enabled: osId > 0,
  })
}

// ---------- relatórios ----------

export function useFaturamento(inicio: string, fim: string) {
  return useQuery({
    queryKey: queryKeys.faturamento(inicio, fim),
    queryFn: () => relatoriosApi.faturamento(inicio, fim),
    enabled: Boolean(inicio && fim),
  })
}

export function useServicosRanking(inicio: string, fim: string) {
  return useQuery({
    queryKey: queryKeys.servicosRanking(inicio, fim),
    queryFn: () => relatoriosApi.servicosMaisRealizados(inicio, fim),
    enabled: Boolean(inicio && fim),
  })
}

export function useClientesRanking(inicio: string, fim: string) {
  return useQuery({
    queryKey: queryKeys.clientesRanking(inicio, fim),
    queryFn: () => relatoriosApi.clientesMaisAtendidos(inicio, fim),
    enabled: Boolean(inicio && fim),
  })
}

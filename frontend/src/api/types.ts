export type StatusOS =
  | 'ABERTA'
  | 'EM_ANDAMENTO'
  | 'AGUARDANDO_CLIENTE'
  | 'CONCLUIDA'
  | 'FECHADA'
  | 'CANCELADA'

export type TipoServico = 'REVISAO' | 'TROCA_PECA'

export type FormaPagamento =
  | 'DINHEIRO'
  | 'PIX'
  | 'CARTAO_CREDITO'
  | 'CARTAO_DEBITO'
  | 'BOLETO'
  | 'OUTRO'

/** Envelope de paginação da API. */
export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type Cliente = {
  id: number
  nome: string
  cpfCnpj: string
  contato: string
  proximaRevisao: string | null
}

export type ClienteRequest = {
  nome: string
  cpfCnpj: string
  contato: string
  proximaRevisao?: string | null
}

export type Moto = {
  id: number
  marca: string
  modelo: string
  cilindradas: number
}

export type MotoRequest = Omit<Moto, 'id'>

export type Peca = {
  id: number
  codigo: string
  descricao: string
  valor: number
  quantidadeEstoque: number
  estoqueMinimo: number
}

export type PecaRequest = Omit<Peca, 'id'>

export type EntradaEstoqueRequest = {
  quantidade: number
}

export type OrdemDeServico = {
  id: number
  clienteId: number
  clienteNome: string
  motoId: number
  motoMarca: string
  motoModelo: string
  motoCilindradas: number
  status: StatusOS
  dataAbertura: string
  dataFechamento: string | null
  formaPagamento: FormaPagamento | null
  totalPecas: number | null
  totalMaoDeObra: number | null
  descontoAplicado: number | null
  totalFinal: number | null
  servicos: Servico[]
}

export type Servico = {
  id: number
  tipo: TipoServico
  descricao: string
  tempoEstimado: number | null
  valorBaseMaoDeObra: number
  valorFinal: number
  pecaId: number | null
  pecaFornecidaPelaLoja: boolean | null
}

export type OrdemDeServicoRequest = {
  clienteId: number
  marcaMoto: string
  modeloMoto: string
}

export type ServicoRequest = {
  tipo: TipoServico
  descricao: string
  tempoEstimado?: number | null
  valorBaseMaoDeObra: number
  checklistGeral?: boolean | null
  pecaId?: number | null
  pecaFornecidaPelaLoja?: boolean | null
}

export type Orcamento = {
  totalPecas: number
  totalMaoDeObra: number
  descontoAplicado: number
  totalFinal: number
}

export type HistoricoStatus = {
  id: number
  osId: number
  statusAnterior: StatusOS | null
  statusNovo: StatusOS
  dataHora: string
}

export type Faturamento = {
  inicio: string
  fim: string
  totalPecas: number
  totalMaoDeObra: number
  descontoAplicado: number
  totalFinal: number
  ticketMedio: number
  quantidadeOs: number
}

export type ServicoRanking = {
  descricao: string
  tipo: TipoServico
  quantidade: number
  totalBaseMaoDeObra: number
}

export type ClienteRanking = {
  clienteId: number
  clienteNome: string
  quantidadeOs: number
  totalFinal: number
}

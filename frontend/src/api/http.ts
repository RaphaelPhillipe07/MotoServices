import { getToken, limparSessao } from '../lib/session'

export type ProblemFieldError = {
  field: string
  message: string
}

export type ProblemDetail = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: ProblemFieldError[]
}

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetail

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  /** Erros por campo, prontos para ligar ao formulário. */
  get fieldErrors(): Record<string, string> {
    const resultado: Record<string, string> = {}
    for (const erro of this.problem?.errors ?? []) {
      resultado[erro.field] = resultado[erro.field]
        ? `${resultado[erro.field]} ${erro.message}`
        : erro.message
    }
    return resultado
  }

  get isNetworkError(): boolean {
    return this.status === 0
  }
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '')

type QueryValue = string | number | boolean | null | undefined

/** Monta a query string ignorando valores vazios. */
export function buildQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams()
  for (const [chave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== null && valor !== '') {
      search.set(chave, String(valor))
    }
  }
  const resultado = search.toString()
  return resultado ? `?${resultado}` : ''
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  } catch {
    throw new ApiError(
      'Não foi possível conectar à API. Verifique se o servidor está em execução.',
      0,
    )
  }

  if (response.status === 401 && token) {
    limparSessao()
  }

  if (response.status === 204) {
    return undefined as T
  }

  const tipoConteudo = response.headers.get('content-type') ?? ''
  const payload = tipoConteudo.includes('json')
    ? await response.json().catch(() => undefined)
    : undefined

  if (!response.ok) {
    throw criarErro(response.status, tipoConteudo, payload)
  }

  return payload as T
}

function criarErro(status: number, tipoConteudo: string, payload: unknown): ApiError {
  const problem = tipoConteudo.includes('problem+json')
    ? (payload as ProblemDetail | undefined)
    : undefined
  const mensagem =
    problem?.detail ??
    problem?.title ??
    `Falha na requisição (HTTP ${status}).`
  return new ApiError(mensagem, status, problem)
}

/** Baixa um recurso binário (CSV/HTML) enviando o token no header Authorization. */
export async function apiDownload(path: string): Promise<Blob> {
  const headers = new Headers()
  headers.set('Accept', 'text/csv, text/html, application/problem+json')
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { headers })
  } catch {
    throw new ApiError(
      'Não foi possível conectar à API. Verifique se o servidor está em execução.',
      0,
    )
  }

  if (response.status === 401 && token) {
    limparSessao()
  }

  if (!response.ok) {
    const tipoConteudo = response.headers.get('content-type') ?? ''
    const payload = tipoConteudo.includes('json')
      ? await response.json().catch(() => undefined)
      : undefined
    throw criarErro(response.status, tipoConteudo, payload)
  }

  return response.blob()
}

/** Salva um recurso autenticado como arquivo, sem expor o token na URL. */
export async function baixarArquivo(path: string, nome: string): Promise<void> {
  const blob = await apiDownload(path)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = nome
  link.rel = 'noreferrer'
  link.click()
  URL.revokeObjectURL(url)
}

/** Abre um recurso autenticado (ex.: comprovante HTML) em nova aba via blob. */
export async function abrirArquivo(path: string): Promise<void> {
  const blob = await apiDownload(path)
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank', 'noopener,noreferrer')
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

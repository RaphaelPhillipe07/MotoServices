import { useSyncExternalStore } from 'react'

export type Papel = 'ADMIN' | 'ATENDENTE' | 'MECANICO'

export type UsuarioSessao = {
  id: number
  username: string
  nome: string
  papel: Papel
}

export type RespostaLogin = {
  accessToken: string
  tokenType: string
  expiresIn: number
  usuario: UsuarioSessao
}

export type Sessao = RespostaLogin & {
  /** Instante local em que o token deixa de ser válido. */
  expiraEm: number
}

const CHAVE = 'motoservices.sessao'
const ouvintes = new Set<() => void>()
let cache: Sessao | null | undefined

function notificar() {
  for (const ouvinte of ouvintes) {
    ouvinte()
  }
}

function lerArmazenado(): Sessao | null {
  try {
    const bruto = window.localStorage.getItem(CHAVE)
    if (!bruto) {
      return null
    }
    const sessao = JSON.parse(bruto) as Sessao
    if (!sessao?.accessToken || !sessao.usuario || typeof sessao.expiraEm !== 'number') {
      return null
    }
    if (sessao.expiraEm <= Date.now()) {
      window.localStorage.removeItem(CHAVE)
      return null
    }
    return sessao
  } catch {
    return null
  }
}

export function getSessao(): Sessao | null {
  if (cache === undefined) {
    cache = lerArmazenado()
  }
  return cache
}

export function getToken(): string | null {
  return getSessao()?.accessToken ?? null
}

export function salvarSessao(resposta: RespostaLogin): Sessao {
  const sessao: Sessao = {
    ...resposta,
    expiraEm: Date.now() + resposta.expiresIn * 1000,
  }
  window.localStorage.setItem(CHAVE, JSON.stringify(sessao))
  cache = sessao
  notificar()
  return sessao
}

export function limparSessao() {
  window.localStorage.removeItem(CHAVE)
  cache = null
  notificar()
}

function inscrever(ouvinte: () => void) {
  ouvintes.add(ouvinte)
  return () => {
    ouvintes.delete(ouvinte)
  }
}

export function useSessao(): Sessao | null {
  return useSyncExternalStore(inscrever, getSessao, () => null)
}

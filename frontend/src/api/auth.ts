import type { RespostaLogin, UsuarioSessao } from '../lib/session'
import { apiRequest } from './http'

export function login(username: string, senha: string) {
  return apiRequest<RespostaLogin>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, senha }),
  })
}

export function buscarUsuarioAtual() {
  return apiRequest<UsuarioSessao>('/auth/me')
}

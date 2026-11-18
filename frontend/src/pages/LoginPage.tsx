import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { ApiError } from '../api/http'
import logoMark from '../assets/logo-mark.png'
import { Button } from '../components/ui/Button'
import { FeedbackMessage } from '../components/ui/States'
import { TextField } from '../components/ui/TextField'
import { salvarSessao, useSessao } from '../lib/session'

export function LoginPage() {
  const sessao = useSessao()
  const navigate = useNavigate()
  const localizacao = useLocation()
  const destino = (localizacao.state as { de?: string } | null)?.de ?? '/painel'

  const [username, setUsername] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (sessao) {
    return <Navigate to={destino} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      const resposta = await login(username.trim(), senha)
      salvarSessao(resposta)
      navigate(destino, { replace: true })
    } catch (error) {
      setErro(
        error instanceof ApiError
          ? error.message
          : 'Não foi possível entrar. Tente novamente.',
      )
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="grid min-h-dvh place-items-center bg-canvas px-5 py-10">
      <main className="w-full max-w-md">
        <Link to="/" className="flex items-center gap-2.5" aria-label="MotoServices, início">
          <img src={logoMark} alt="" className="size-10" />
          <span className="flex flex-col leading-tight">
            <span className="text-[15px] font-semibold tracking-tight">MotoServices</span>
            <span className="text-[10px] font-semibold tracking-[0.16em] text-ink-muted uppercase">
              Oficina
            </span>
          </span>
        </Link>

        <div className="mt-8 rounded-2xl border border-border bg-surface p-6 shadow-sm sm:p-8">
          <h1 className="text-2xl font-semibold tracking-tight">Entrar no painel</h1>
          <p className="mt-2 text-sm text-ink-muted">
            Acesso restrito à equipe da oficina. Use o usuário e a senha fornecidos pelo
            administrador.
          </p>

          <form onSubmit={(event) => void handleSubmit(event)} className="mt-6 space-y-4">
            <TextField
              label="Usuário"
              name="username"
              autoComplete="username"
              autoFocus
              required
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
            <TextField
              label="Senha"
              name="senha"
              type="password"
              autoComplete="current-password"
              required
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
            />

            {erro && <FeedbackMessage tone="error">{erro}</FeedbackMessage>}

            <Button type="submit" size="lg" className="w-full" loading={enviando}>
              Entrar
            </Button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-ink-muted">
          <Link to="/" className="underline underline-offset-2 hover:text-ink">
            Voltar ao site
          </Link>
        </p>
      </main>
    </div>
  )
}

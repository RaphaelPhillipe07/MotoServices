import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="max-w-2xl">
      <p className="text-sm font-semibold text-primary">404</p>
      <h1 className="mt-1 text-2xl font-semibold tracking-tight">Página não encontrada</h1>
      <p className="mt-2 text-ink-muted">
        O endereço acessado não existe. Verifique o link ou volte para o painel.
      </p>
      <Link
        to="/painel"
        className="mt-6 inline-flex rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-primary hover:bg-primary-hover"
      >
        Voltar ao painel
      </Link>
    </div>
  )
}

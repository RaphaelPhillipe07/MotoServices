import { Link } from 'react-router-dom'

type PlaceholderPageProps = {
  title: string
  description: string
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-2 text-ink-muted">{description}</p>

      <div className="mt-6 rounded-xl border border-dashed border-border-strong bg-surface p-8 text-center">
        <p className="font-medium">Em construção</p>
        <p className="mt-1 text-sm text-ink-muted">
          Esta área será liberada no próximo passo da fase de interface.
        </p>
        <Link
          to="/painel"
          className="mt-4 inline-flex rounded-md px-3 py-2 text-sm font-medium text-primary hover:bg-primary-surface"
        >
          Voltar ao painel
        </Link>
      </div>
    </div>
  )
}

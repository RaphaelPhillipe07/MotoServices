import { Button } from './Button'

type PaginationProps = {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
  itemLabel?: string
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
  itemLabel = 'itens',
}: PaginationProps) {
  if (totalElements === 0) return null
  const total = Math.max(totalPages, 1)

  return (
    <nav aria-label="Paginação" className="mt-4 flex flex-wrap items-center justify-between gap-3">
      <p aria-live="polite" className="text-sm text-ink-muted">
        Página {page + 1} de {total} · {totalElements} {itemLabel}
      </p>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
        >
          Anterior
        </Button>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= total}
        >
          Próxima
        </Button>
      </div>
    </nav>
  )
}

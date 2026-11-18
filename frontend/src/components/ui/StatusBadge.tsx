import type { StatusOS } from '../../api/types'
import { statusMeta } from '../../lib/status'
import { Badge } from './Badge'

export function StatusBadge({ status }: { status: StatusOS }) {
  const { label, variant, icon: Icon } = statusMeta[status]
  return (
    <Badge variant={variant}>
      <Icon aria-hidden="true" className="size-3.5" />
      {label}
    </Badge>
  )
}

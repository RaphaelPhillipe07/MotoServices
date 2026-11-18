import { BarChart3, Bike, ClipboardList, Package, Users } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Papel } from '../../lib/session'

export type NavigationItem = {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
  /** Quando definido, o item só aparece para estes papéis. */
  papeis?: Papel[]
}

export const navigation: NavigationItem[] = [
  { to: '/painel', label: 'Painel', icon: ClipboardList, end: true },
  { to: '/ordens-servico', label: 'Ordens de serviço', icon: ClipboardList },
  { to: '/clientes', label: 'Clientes', icon: Users },
  { to: '/motos', label: 'Motos', icon: Bike },
  { to: '/estoque', label: 'Estoque', icon: Package },
  {
    to: '/relatorios',
    label: 'Relatórios',
    icon: BarChart3,
    papeis: ['ADMIN', 'ATENDENTE'],
  },
]

const moeda = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

export function formatCurrency(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? '—' : moeda.format(valor)
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const data = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  return Number.isNaN(data.getTime()) ? '—' : data.toLocaleDateString('pt-BR')
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const data = new Date(iso)
  return Number.isNaN(data.getTime())
    ? '—'
    : data.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export function formatEspera(iso: string | null | undefined, agora = Date.now()): string {
  if (!iso) return '—'
  const inicio = new Date(iso).getTime()
  if (Number.isNaN(inicio)) return '—'
  const minutos = Math.max(0, Math.floor((agora - inicio) / 60_000))
  if (minutos < 60) return `${minutos}min`
  const horas = Math.floor(minutos / 60)
  if (horas < 24) return `${horas}h${String(minutos % 60).padStart(2, '0')}`
  const dias = Math.floor(horas / 24)
  return `${dias}d ${horas % 24}h`
}

export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '')
}

export function formatDocumento(value: string | null | undefined): string {
  const digitos = onlyDigits(value ?? '')
  if (digitos.length === 11) {
    return digitos.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
  }
  if (digitos.length === 14) {
    return digitos.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5')
  }
  return value ?? '—'
}

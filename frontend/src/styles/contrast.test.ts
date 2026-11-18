import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * Contraste WCAG 2.2 — pares de tokens usados em texto normal.
 * Alvo: AA (>= 4.5:1) e, quando possível, AAA (>= 7:1).
 */
const css = readFileSync(resolve(process.cwd(), 'src/styles/globals.css'), 'utf8')

function token(nome: string): string {
  const match = css.match(new RegExp(`--${nome}:\\s*(#[0-9a-fA-F]{6})`))
  if (!match) {
    throw new Error(`Token não encontrado: ${nome}`)
  }
  return match[1]
}

function canais(hex: string): [number, number, number] {
  const limpo = hex.replace('#', '')
  return [
    Number.parseInt(limpo.slice(0, 2), 16),
    Number.parseInt(limpo.slice(2, 4), 16),
    Number.parseInt(limpo.slice(4, 6), 16),
  ]
}

function luminancia(hex: string): number {
  const [r, g, b] = canais(hex).map((canal) => {
    const s = canal / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contraste(fundo: string, texto: string): number {
  const l1 = luminancia(fundo)
  const l2 = luminancia(texto)
  const maisClaro = Math.max(l1, l2)
  const maisEscuro = Math.min(l1, l2)
  return (maisClaro + 0.05) / (maisEscuro + 0.05)
}

const pares: { nome: string; fundo: string; texto: string }[] = [
  { nome: 'texto principal no canvas', fundo: token('color-canvas'), texto: token('color-ink') },
  { nome: 'texto principal na superfície', fundo: token('color-surface'), texto: token('color-ink') },
  { nome: 'texto secundário na superfície', fundo: token('color-surface'), texto: token('color-ink-muted') },
  { nome: 'ação principal sobre superfície clara', fundo: token('color-primary-surface'), texto: token('color-primary') },
  { nome: 'texto do botão primário', fundo: token('color-primary'), texto: token('color-on-primary') },
  { nome: 'texto do destaque', fundo: token('color-accent'), texto: token('color-on-accent') },
  { nome: 'erro sobre superfície de erro', fundo: token('color-danger-surface'), texto: token('color-danger') },
  { nome: 'sucesso sobre superfície de sucesso', fundo: token('color-success-surface'), texto: token('color-success') },
  { nome: 'aviso sobre superfície de aviso', fundo: token('color-warning-surface'), texto: token('color-warning') },
  { nome: 'informação sobre superfície de informação', fundo: token('color-info-surface'), texto: token('color-info') },
]

describe('contraste dos tokens de cor', () => {
  it.each(pares)('$nome atende WCAG AA (4.5:1)', ({ fundo, texto }) => {
    expect(contraste(fundo, texto)).toBeGreaterThanOrEqual(4.5)
  })
})

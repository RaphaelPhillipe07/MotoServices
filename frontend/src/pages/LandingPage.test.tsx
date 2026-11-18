import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/utils'
import { LandingPage } from './LandingPage'
import { depoimentoMock, perguntasFrequentesMock } from './landingContent'

describe('LandingPage', () => {
  it('exibe o conteúdo mockado de depoimento e FAQ', () => {
    renderWithProviders(<LandingPage />)

    expect(
      screen.getByRole('heading', { level: 1, name: /a oficina inteira/i }),
    ).toBeInTheDocument()
    expect(screen.getAllByText(/conteúdo de exemplo/i).length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText(depoimentoMock.citacao, { exact: false })).toBeInTheDocument()
    expect(screen.getByText(depoimentoMock.assinatura)).toBeInTheDocument()

    for (const { pergunta, resposta } of perguntasFrequentesMock) {
      expect(screen.getByText(pergunta)).toBeInTheDocument()
      expect(screen.getByText(resposta)).toBeInTheDocument()
    }
  })

  it('aponta o menu e o CTA final para os destinos corretos', () => {
    renderWithProviders(<LandingPage />)

    expect(screen.getByRole('link', { name: 'Perguntas' })).toHaveAttribute('href', '#faq')
    expect(screen.getByRole('link', { name: /cadastrar a primeira moto/i })).toHaveAttribute(
      'href',
      '/painel',
    )
    expect(screen.getByText('Dados de exemplo')).toBeInTheDocument()
  })
})

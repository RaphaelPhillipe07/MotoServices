import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { login } from '../api/auth'
import { getSessao } from '../lib/session'
import { renderWithProviders } from '../test/utils'
import { LoginPage } from './LoginPage'

vi.mock('../api/auth', () => ({
  login: vi.fn(),
}))

const loginMock = vi.mocked(login)

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/entrar" element={<LoginPage />} />
      <Route path="/painel" element={<p>Painel autenticado</p>} />
    </Routes>,
    { route: '/entrar', autenticado: false },
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    loginMock.mockReset()
  })

  it('autentica, guarda a sessão e segue para o painel', async () => {
    loginMock.mockResolvedValue({
      accessToken: 'jwt-de-teste',
      tokenType: 'Bearer',
      expiresIn: 3600,
      usuario: { id: 7, username: 'admin', nome: 'Administrador', papel: 'ADMIN' },
    })

    renderLogin()

    expect(screen.getByRole('heading', { name: /entrar no painel/i })).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText(/usuário/i), 'admin')
    await userEvent.type(screen.getByLabelText(/senha/i), 'admin123')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    expect(await screen.findByText('Painel autenticado')).toBeInTheDocument()
    expect(loginMock).toHaveBeenCalledWith('admin', 'admin123')
    expect(getSessao()?.usuario.username).toBe('admin')
  })

  it('mostra a mensagem do servidor quando as credenciais são inválidas', async () => {
    loginMock.mockRejectedValue(new Error('Usuário ou senha inválidos.'))

    renderLogin()

    await userEvent.type(screen.getByLabelText(/usuário/i), 'admin')
    await userEvent.type(screen.getByLabelText(/senha/i), 'errada')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Não foi possível entrar. Tente novamente.',
    )
    expect(getSessao()).toBeNull()
  })
})

import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest, buildQuery } from './http'

const fetchMock = vi.fn()
vi.stubGlobal('fetch', fetchMock)

function resposta(body: unknown, status = 200, tipo = 'application/json') {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': tipo }),
    json: async () => body,
  } as Response
}

afterEach(() => {
  fetchMock.mockReset()
})

describe('buildQuery', () => {
  it('monta a query string ignorando valores vazios', () => {
    expect(buildQuery({ page: 0, size: 10, busca: '', termo: undefined, ativo: true })).toBe(
      '?page=0&size=10&ativo=true',
    )
  })

  it('retorna vazio quando não há parâmetros', () => {
    expect(buildQuery({})).toBe('')
  })
})

describe('apiRequest', () => {
  it('devolve o JSON em caso de sucesso', async () => {
    fetchMock.mockResolvedValue(resposta({ id: 1, nome: 'Maria' }))

    await expect(apiRequest('/clientes/1')).resolves.toEqual({ id: 1, nome: 'Maria' })
    expect(fetchMock).toHaveBeenCalledWith('/api/clientes/1', expect.any(Object))
  })

  it('trata 204 sem corpo', async () => {
    fetchMock.mockResolvedValue(resposta(undefined, 204, ''))

    await expect(apiRequest('/clientes/1/motos/2')).resolves.toBeUndefined()
  })

  it('mapeia ProblemDetail e os erros por campo', async () => {
    fetchMock.mockResolvedValue(
      resposta(
        {
          type: 'urn:motoservices:problem:validacao',
          title: 'Dados inválidos',
          status: 400,
          detail: 'Um ou mais campos estão inválidos.',
          errors: [
            { field: 'nome', message: 'nome é obrigatório' },
            { field: 'cpfCnpj', message: 'CPF/CNPJ inválido' },
          ],
        },
        400,
        'application/problem+json',
      ),
    )

    const erro = await apiRequest('/clientes').catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ApiError)
    const apiError = erro as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.message).toBe('Um ou mais campos estão inválidos.')
    expect(apiError.fieldErrors).toEqual({
      nome: 'nome é obrigatório',
      cpfCnpj: 'CPF/CNPJ inválido',
    })
  })

  it('transforma falha de rede em ApiError com status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const erro = await apiRequest('/clientes').catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ApiError)
    expect((erro as ApiError).status).toBe(0)
    expect((erro as ApiError).isNetworkError).toBe(true)
  })

  it('envia Accept e Content-Type JSON em requisições com corpo', async () => {
    fetchMock.mockResolvedValue(resposta({ id: 1 }, 201))

    await apiRequest('/clientes', { method: 'POST', body: JSON.stringify({ nome: 'Maria' }) })

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit
    const headers = init.headers as Headers
    expect(headers.get('Accept')).toContain('application/problem+json')
    expect(headers.get('Content-Type')).toBe('application/json')
  })
})

import { Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useBuscaClientes, useCriarOrdem, useMotos } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Cliente } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { FeedbackMessage } from '../../components/ui/States'
import { SelectField, TextField } from '../../components/ui/TextField'
import { formatDocumento } from '../../lib/format'

type OrdemFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function OrdemFormDialog({ open, onOpenChange }: OrdemFormDialogProps) {
  const navigate = useNavigate()
  const criarOrdem = useCriarOrdem()
  const [termo, setTermo] = useState('')
  const [busca, setBusca] = useState('')
  const [cliente, setCliente] = useState<Cliente | null>(null)
  const [motoId, setMotoId] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const clientes = useBuscaClientes(busca, 0, 5)
  const motos = useMotos(0, 100)
  const motoSelecionada = motos.data?.content.find((moto) => moto.id === Number(motoId))

  useEffect(() => {
    if (!open) {
      setTermo('')
      setBusca('')
      setCliente(null)
      setMotoId('')
      setErro(null)
    }
  }, [open])

  async function criar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErro(null)
    if (!cliente) {
      setErro('Selecione o cliente da OS.')
      return
    }
    if (!motoSelecionada) {
      setErro('Selecione a moto da OS.')
      return
    }
    try {
      const os = await criarOrdem.mutateAsync({
        clienteId: cliente.id,
        marcaMoto: motoSelecionada.marca,
        modeloMoto: motoSelecionada.modelo,
      })
      onOpenChange(false)
      navigate(`/ordens-servico/${os.id}`)
    } catch (error) {
      setErro(
        error instanceof ApiError ? error.message : 'Não foi possível abrir a ordem de serviço.',
      )
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        title="Nova ordem de serviço"
        description="Escolha o cliente e a moto; o vínculo cliente-moto é criado automaticamente."
      >
        <form onSubmit={criar} noValidate className="space-y-4">
          {erro && <FeedbackMessage tone="error">{erro}</FeedbackMessage>}

          <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
            <div className="flex-1">
              <TextField
                label="Buscar cliente"
                placeholder="Nome, CPF ou CNPJ"
                value={termo}
                onChange={(event) => setTermo(event.target.value)}
              />
            </div>
            <Button type="button" variant="secondary" onClick={(event) => {
              event.preventDefault()
              setBusca(termo.trim())
            }}>
              <Search aria-hidden="true" className="size-4" />
              Buscar
            </Button>
          </div>

          {busca && clientes.isLoading && (
            <p role="status" className="text-sm text-ink-muted">
              Buscando clientes…
            </p>
          )}
          {busca && clientes.isError && (
            <FeedbackMessage tone="error">
              Não foi possível buscar clientes. Tente novamente.
            </FeedbackMessage>
          )}
          {busca && clientes.data && clientes.data.content.length === 0 && (
            <p className="text-sm text-ink-muted">
              Nenhum cliente encontrado. Cadastre o cliente na área de Clientes.
            </p>
          )}

          {clientes.data && clientes.data.content.length > 0 && (
            <fieldset className="space-y-2">
              <legend className="text-sm font-medium">Cliente</legend>
              {clientes.data.content.map((encontrado) => (
                <label
                  key={encontrado.id}
                  className="flex items-start gap-2 rounded-md border border-border bg-surface p-2 text-sm"
                >
                  <input
                    type="radio"
                    name="cliente"
                    value={encontrado.id}
                    checked={cliente?.id === encontrado.id}
                    onChange={() => setCliente(encontrado)}
                    className="mt-1"
                  />
                  <span>
                    <span className="font-medium">{encontrado.nome}</span>
                    <span className="block text-xs text-ink-muted">
                      {formatDocumento(encontrado.cpfCnpj)}
                    </span>
                  </span>
                </label>
              ))}
            </fieldset>
          )}

          <SelectField
            label="Moto"
            required
            value={motoId}
            onChange={(event) => setMotoId(event.target.value)}
            hint={motos.isLoading ? 'Carregando catálogo…' : 'Motos cadastradas no catálogo.'}
          >
            <option value="">Selecione…</option>
            {(motos.data?.content ?? []).map((moto) => (
              <option key={moto.id} value={moto.id}>
                {moto.marca} {moto.modelo} ({moto.cilindradas} cc)
              </option>
            ))}
          </SelectField>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={criarOrdem.isPending} disabled={!cliente || !motoId}>
              Abrir OS
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

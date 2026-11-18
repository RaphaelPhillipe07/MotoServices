import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useEntradaEstoque } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Peca } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { FeedbackMessage } from '../../components/ui/States'
import { TextField } from '../../components/ui/TextField'

const entradaSchema = z.object({
  quantidade: z
    .number({ error: 'Informe a quantidade.' })
    .int('Use um número inteiro.')
    .positive('A quantidade deve ser positiva.'),
})

type EntradaFormValues = z.infer<typeof entradaSchema>

type EntradaEstoqueDialogProps = {
  peca: Peca | null
  onOpenChange: (open: boolean) => void
  onUpdated?: (mensagem: string) => void
}

export function EntradaEstoqueDialog({ peca, onOpenChange, onUpdated }: EntradaEstoqueDialogProps) {
  const entrada = useEntradaEstoque()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<EntradaFormValues>({
    resolver: zodResolver(entradaSchema),
    defaultValues: { quantidade: undefined },
  })

  useEffect(() => {
    if (!peca) {
      reset()
      setErroGeral(null)
    }
  }, [peca, reset])

  const quantidade = watch('quantidade')
  const saldoAtual = peca?.quantidadeEstoque ?? 0
  const novoSaldo =
    Number.isFinite(quantidade) && quantidade > 0 ? saldoAtual + quantidade : saldoAtual

  const onSubmit = handleSubmit(async (values) => {
    if (!peca) return
    setErroGeral(null)
    try {
      await entrada.mutateAsync({ pecaId: peca.id, quantidade: values.quantidade })
      onUpdated?.(`Entrada de ${values.quantidade} registrada para ${peca.codigo}.`)
      onOpenChange(false)
    } catch (error) {
      setErroGeral(
        error instanceof ApiError ? error.message : 'Não foi possível registrar a entrada.',
      )
    }
  })

  return (
    <Dialog open={peca !== null} onOpenChange={onOpenChange}>
      <DialogContent
        title="Entrada de estoque"
        description={
          peca
            ? `Peça ${peca.codigo} — ${peca.descricao}. Saldo atual: ${saldoAtual} unidade(s).`
            : undefined
        }
      >
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          {erroGeral && <FeedbackMessage tone="error">{erroGeral}</FeedbackMessage>}

          <TextField
            label="Quantidade recebida"
            required
            type="number"
            min={1}
            step={1}
            error={errors.quantidade?.message}
            {...register('quantidade', { valueAsNumber: true })}
          />

          <p aria-live="polite" className="text-sm text-ink-muted">
            Novo saldo: <span className="tnum font-medium text-ink">{novoSaldo}</span> unidade(s).
          </p>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Registrar entrada
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

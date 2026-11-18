import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useCriarPeca } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Peca } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { FeedbackMessage } from '../../components/ui/States'
import { TextField } from '../../components/ui/TextField'

const pecaSchema = z.object({
  codigo: z.string().trim().min(1, 'Informe o código.'),
  descricao: z.string().trim().min(3, 'Informe a descrição.'),
  valor: z.number({ error: 'Informe o valor.' }).min(0, 'O valor não pode ser negativo.'),
  quantidadeEstoque: z
    .number({ error: 'Informe a quantidade.' })
    .int('Use um número inteiro.')
    .min(0, 'A quantidade não pode ser negativa.'),
  estoqueMinimo: z
    .number({ error: 'Informe o estoque mínimo.' })
    .int('Use um número inteiro.')
    .min(0, 'O estoque mínimo não pode ser negativo.'),
})

type PecaFormValues = z.infer<typeof pecaSchema>

const camposConhecidos: (keyof PecaFormValues)[] = [
  'codigo',
  'descricao',
  'valor',
  'quantidadeEstoque',
  'estoqueMinimo',
]

type PecaFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated?: (peca: Peca) => void
}

export function PecaFormDialog({ open, onOpenChange, onCreated }: PecaFormDialogProps) {
  const criarPeca = useCriarPeca()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PecaFormValues>({
    resolver: zodResolver(pecaSchema),
    defaultValues: {
      codigo: '',
      descricao: '',
      valor: undefined,
      quantidadeEstoque: undefined,
      estoqueMinimo: undefined,
    },
  })

  useEffect(() => {
    if (!open) {
      reset()
      setErroGeral(null)
    }
  }, [open, reset])

  const onSubmit = handleSubmit(async (values) => {
    setErroGeral(null)
    try {
      const criada = await criarPeca.mutateAsync({
        codigo: values.codigo.trim(),
        descricao: values.descricao.trim(),
        valor: values.valor,
        quantidadeEstoque: values.quantidadeEstoque,
        estoqueMinimo: values.estoqueMinimo,
      })
      onCreated?.(criada)
      onOpenChange(false)
    } catch (error) {
      if (error instanceof ApiError) {
        const campos = error.fieldErrors
        let aplicouCampo = false
        for (const campo of camposConhecidos) {
          if (campos[campo]) {
            setError(campo, { type: 'server', message: campos[campo] })
            aplicouCampo = true
          }
        }
        setErroGeral(aplicouCampo ? null : error.message)
      } else {
        setErroGeral('Não foi possível cadastrar a peça.')
      }
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        title="Nova peça"
        description="Cadastre a peça no estoque e defina o alerta de ruptura."
      >
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          {erroGeral && <FeedbackMessage tone="error">{erroGeral}</FeedbackMessage>}

          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="Código"
              required
              error={errors.codigo?.message}
              {...register('codigo')}
            />
            <TextField
              label="Valor unitário"
              required
              type="number"
              min={0}
              step="0.01"
              inputMode="decimal"
              error={errors.valor?.message}
              {...register('valor', { valueAsNumber: true })}
            />
          </div>

          <TextField
            label="Descrição"
            required
            error={errors.descricao?.message}
            {...register('descricao')}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="Quantidade em estoque"
              required
              type="number"
              min={0}
              step={1}
              error={errors.quantidadeEstoque?.message}
              {...register('quantidadeEstoque', { valueAsNumber: true })}
            />
            <TextField
              label="Estoque mínimo"
              required
              type="number"
              min={0}
              step={1}
              hint="Dispara o alerta de ruptura."
              error={errors.estoqueMinimo?.message}
              {...register('estoqueMinimo', { valueAsNumber: true })}
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Salvar peça
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

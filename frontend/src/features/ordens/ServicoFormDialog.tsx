import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useAdicionarServico, usePecas } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { ServicoRequest } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { FeedbackMessage } from '../../components/ui/States'
import { SelectField, TextField } from '../../components/ui/TextField'
import { formatCurrency } from '../../lib/format'

const servicoSchema = z
  .object({
    tipo: z.enum(['REVISAO', 'TROCA_PECA']),
    descricao: z.string().trim().min(3, 'Informe a descrição.'),
    valorBaseMaoDeObra: z
      .number({ error: 'Informe a mão de obra.' })
      .min(0, 'A mão de obra não pode ser negativa.'),
    tempoEstimado: z.number().int().min(0).optional(),
    checklistGeral: z.boolean().optional(),
    pecaFornecidaPelaLoja: z.boolean().optional(),
    pecaId: z.number().int().positive().optional(),
  })
  .superRefine((values, ctx) => {
    if (values.tipo !== 'TROCA_PECA') return
    if (values.pecaFornecidaPelaLoja === undefined) {
      ctx.addIssue({
        code: 'custom',
        path: ['pecaFornecidaPelaLoja'],
        message: 'Informe a origem da peça.',
      })
    }
    if (values.pecaFornecidaPelaLoja && !values.pecaId) {
      ctx.addIssue({ code: 'custom', path: ['pecaId'], message: 'Selecione a peça.' })
    }
  })

type ServicoFormValues = z.infer<typeof servicoSchema>

const camposConhecidos: (keyof ServicoFormValues)[] = [
  'tipo',
  'descricao',
  'valorBaseMaoDeObra',
  'pecaFornecidaPelaLoja',
  'pecaId',
]

type ServicoFormDialogProps = {
  osId: number
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated?: (mensagem: string) => void
}

export function ServicoFormDialog({ osId, open, onOpenChange, onCreated }: ServicoFormDialogProps) {
  const adicionar = useAdicionarServico(osId)
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ServicoFormValues>({
    resolver: zodResolver(servicoSchema),
    defaultValues: {
      tipo: 'REVISAO',
      descricao: '',
      valorBaseMaoDeObra: undefined,
      tempoEstimado: undefined,
      checklistGeral: false,
      pecaFornecidaPelaLoja: undefined,
      pecaId: undefined,
    },
  })

  const tipo = watch('tipo')
  const pecaDaLoja = watch('pecaFornecidaPelaLoja')
  const pecas = usePecas('', 0, 100, tipo === 'TROCA_PECA' && pecaDaLoja === true)

  useEffect(() => {
    if (!open) {
      reset()
      setErroGeral(null)
    }
  }, [open, reset])

  const onSubmit = handleSubmit(async (values) => {
    setErroGeral(null)
    const payload: ServicoRequest = {
      tipo: values.tipo,
      descricao: values.descricao.trim(),
      valorBaseMaoDeObra: values.valorBaseMaoDeObra,
      tempoEstimado: values.tempoEstimado ?? null,
      checklistGeral: values.tipo === 'REVISAO' ? Boolean(values.checklistGeral) : null,
      pecaFornecidaPelaLoja:
        values.tipo === 'TROCA_PECA' ? Boolean(values.pecaFornecidaPelaLoja) : null,
      pecaId: values.tipo === 'TROCA_PECA' ? (values.pecaId ?? null) : null,
    }

    try {
      await adicionar.mutateAsync(payload)
      onCreated?.('Serviço adicionado ao orçamento.')
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
        setErroGeral('Não foi possível adicionar o serviço.')
      }
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        title="Adicionar serviço"
        description="Revisões e trocas de peça entram no orçamento com o fator de risco da moto."
      >
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          {erroGeral && <FeedbackMessage tone="error">{erroGeral}</FeedbackMessage>}

          <SelectField label="Tipo de serviço" required error={errors.tipo?.message} {...register('tipo')}>
            <option value="REVISAO">Revisão</option>
            <option value="TROCA_PECA">Troca de peça</option>
          </SelectField>

          <TextField
            label="Descrição"
            required
            error={errors.descricao?.message}
            {...register('descricao')}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="Mão de obra (R$)"
              required
              type="number"
              min={0}
              step="0.01"
              error={errors.valorBaseMaoDeObra?.message}
              {...register('valorBaseMaoDeObra', { valueAsNumber: true })}
            />
            <TextField
              label="Tempo estimado (min)"
              type="number"
              min={0}
              step={1}
              error={errors.tempoEstimado?.message}
              {...register('tempoEstimado', {
                setValueAs: (valor) => (valor === '' ? undefined : Number(valor)),
              })}
            />
          </div>

          {tipo === 'REVISAO' && (
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" className="size-4" {...register('checklistGeral')} />
              Checklist geral concluído
            </label>
          )}

          {tipo === 'TROCA_PECA' && (
            <>
              <SelectField
                label="Origem da peça"
                required
                error={errors.pecaFornecidaPelaLoja?.message}
                {...register('pecaFornecidaPelaLoja', {
                  setValueAs: (valor) => (valor === '' ? undefined : valor === 'true'),
                })}
              >
                <option value="">Selecione…</option>
                <option value="true">Fornecida pela loja</option>
                <option value="false">Trazida pelo cliente</option>
              </SelectField>

              {pecaDaLoja === true && (
                <SelectField
                  label="Peça"
                  required
                  hint={
                    pecas.isLoading
                      ? 'Carregando peças…'
                      : 'Peças cadastradas no estoque.'
                  }
                  error={errors.pecaId?.message}
                  {...register('pecaId', {
                    setValueAs: (valor) => (valor === '' ? undefined : Number(valor)),
                  })}
                >
                  <option value="">Selecione…</option>
                  {(pecas.data?.content ?? []).map((peca) => (
                    <option key={peca.id} value={peca.id}>
                      {peca.codigo} — {peca.descricao} ({formatCurrency(peca.valor)})
                    </option>
                  ))}
                </SelectField>
              )}
            </>
          )}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Adicionar serviço
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

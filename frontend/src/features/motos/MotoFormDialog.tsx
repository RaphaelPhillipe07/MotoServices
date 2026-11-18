import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useCriarMoto } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Moto } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { FeedbackMessage } from '../../components/ui/States'
import { TextField } from '../../components/ui/TextField'

const motoSchema = z.object({
  marca: z.string().trim().min(2, 'Informe a marca.'),
  modelo: z.string().trim().min(1, 'Informe o modelo.'),
  cilindradas: z
    .number({ error: 'Informe as cilindradas.' })
    .int('Use um número inteiro.')
    .positive('Cilindradas devem ser positivas.'),
})

type MotoFormValues = z.infer<typeof motoSchema>

const camposConhecidos: (keyof MotoFormValues)[] = ['marca', 'modelo', 'cilindradas']

type MotoFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated?: (moto: Moto) => void
}

export function MotoFormDialog({ open, onOpenChange, onCreated }: MotoFormDialogProps) {
  const criarMoto = useCriarMoto()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<MotoFormValues>({
    resolver: zodResolver(motoSchema),
    defaultValues: { marca: '', modelo: '', cilindradas: undefined },
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
      const criada = await criarMoto.mutateAsync({
        marca: values.marca.trim(),
        modelo: values.modelo.trim(),
        cilindradas: values.cilindradas,
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
        setErroGeral('Não foi possível cadastrar a moto.')
      }
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        title="Nova moto"
        description="Cadastre marca, modelo e cilindradas no catálogo da oficina."
      >
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          {erroGeral && <FeedbackMessage tone="error">{erroGeral}</FeedbackMessage>}

          <TextField
            label="Marca"
            required
            error={errors.marca?.message}
            {...register('marca')}
          />
          <TextField
            label="Modelo"
            required
            error={errors.modelo?.message}
            {...register('modelo')}
          />
          <TextField
            label="Cilindradas"
            required
            type="number"
            min={1}
            step={1}
            hint="Ex.: 160"
            error={errors.cilindradas?.message}
            {...register('cilindradas', { valueAsNumber: true })}
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Salvar moto
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useCriarCliente } from '../../api/hooks'
import { ApiError } from '../../api/http'
import type { Cliente } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog, DialogContent } from '../../components/ui/Dialog'
import { FeedbackMessage } from '../../components/ui/States'
import { TextField } from '../../components/ui/TextField'
import { onlyDigits } from '../../lib/format'

const clienteSchema = z.object({
  nome: z.string().trim().min(3, 'Informe ao menos 3 caracteres.'),
  cpfCnpj: z
    .string()
    .trim()
    .min(1, 'Informe o CPF ou CNPJ.')
    .refine(
      (valor) => [11, 14].includes(onlyDigits(valor).length),
      'CPF deve ter 11 ou CNPJ 14 dígitos.',
    ),
  contato: z.string().trim().min(8, 'Informe um contato com ao menos 8 caracteres.'),
  proximaRevisao: z.string().optional(),
})

type ClienteFormValues = z.infer<typeof clienteSchema>

const camposConhecidos: (keyof ClienteFormValues)[] = [
  'nome',
  'cpfCnpj',
  'contato',
  'proximaRevisao',
]

type ClienteFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated?: (cliente: Cliente) => void
}

export function ClienteFormDialog({ open, onOpenChange, onCreated }: ClienteFormDialogProps) {
  const criarCliente = useCriarCliente()
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ClienteFormValues>({
    resolver: zodResolver(clienteSchema),
    defaultValues: { nome: '', cpfCnpj: '', contato: '', proximaRevisao: '' },
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
      const criado = await criarCliente.mutateAsync({
        nome: values.nome.trim(),
        cpfCnpj: values.cpfCnpj.trim(),
        contato: values.contato.trim(),
        proximaRevisao: values.proximaRevisao ? values.proximaRevisao : null,
      })
      onCreated?.(criado)
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
        setErroGeral('Não foi possível cadastrar o cliente.')
      }
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        title="Novo cliente"
        description="Cadastre o cliente para vincular motos e abrir ordens de serviço."
      >
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          {erroGeral && <FeedbackMessage tone="error">{erroGeral}</FeedbackMessage>}

          <TextField
            label="Nome"
            required
            autoComplete="name"
            error={errors.nome?.message}
            {...register('nome')}
          />
          <TextField
            label="CPF ou CNPJ"
            required
            inputMode="numeric"
            autoComplete="off"
            hint="Somente números ou com pontuação."
            error={errors.cpfCnpj?.message}
            {...register('cpfCnpj')}
          />
          <TextField
            label="Contato"
            required
            hint="Telefone ou e-mail."
            error={errors.contato?.message}
            {...register('contato')}
          />
          <TextField
            label="Próxima revisão"
            type="date"
            hint="Opcional."
            error={errors.proximaRevisao?.message}
            {...register('proximaRevisao')}
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Salvar cliente
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

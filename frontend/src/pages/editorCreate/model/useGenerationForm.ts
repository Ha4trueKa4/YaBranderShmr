import { DEFAULT_GENERATION_MODEL } from '@features/editorGenerate'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

type GenerationFormValues = { description: string; model: string }

interface UseGenerationFormOptions {
  onSubmit: (values: GenerationFormValues) => void
}

export function useGenerationForm({ onSubmit }: UseGenerationFormOptions) {
  const { t } = useTranslation()
  const generationFormSchema = z.object({
    description: z
      .string()
      .trim()
      .min(1, t('Describe the identity you want to generate.')),
    model: z.string(),
  })
  const form = useForm<GenerationFormValues>({
    defaultValues: { description: '', model: DEFAULT_GENERATION_MODEL },
    mode: 'onSubmit',
    resolver: zodResolver(generationFormSchema),
  })

  return {
    form,
    submit: form.handleSubmit(onSubmit),
  }
}

import { DEFAULT_GENERATION_MODEL } from '@features/editorGenerate'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

const generationFormSchema = z.object({
  description: z
    .string()
    .trim()
    .min(1, 'Describe the identity you want to generate.'),
  model: z.string(),
})

type GenerationFormValues = z.infer<typeof generationFormSchema>

interface UseGenerationFormOptions {
  onSubmit: (values: GenerationFormValues) => void
}

export function useGenerationForm({ onSubmit }: UseGenerationFormOptions) {
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

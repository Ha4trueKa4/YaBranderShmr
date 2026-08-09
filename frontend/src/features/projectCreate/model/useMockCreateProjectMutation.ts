import { useMutation } from '@tanstack/react-query'

interface MockCreateProjectInput {
  description: string
  name: string
}

interface MockCreatedProject {
  id: string
}

interface UseMockCreateProjectMutationOptions {
  onSuccess?: (project: MockCreatedProject) => void
}

const mockRequestDelayMs = 1500

/**
 * Временная мутация для демонстрации потока создания.
 * Заменить на `useCreateProjectMutation`, когда API создания проекта станет доступен.
 */
function mockCreateProject({
  name,
}: MockCreateProjectInput): Promise<MockCreatedProject> {
  return new Promise((resolve) => {
    window.setTimeout(() => {
      resolve({
        id: `${name.toLowerCase().replaceAll(' ', '-')}-${Date.now()}`,
      })
    }, mockRequestDelayMs)
  })
}

export function useMockCreateProjectMutation({
  onSuccess,
}: UseMockCreateProjectMutationOptions) {
  return useMutation({
    mutationKey: ['project', 'create', 'mock'],
    mutationFn: mockCreateProject,
    onSuccess,
  })
}

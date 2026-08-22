import { deleteProject } from '@entities/project'

export async function cancelGeneration(projectId: string) {
  // TODO: Replace project deletion with the API request that cancels the generation job.
  await deleteProject(projectId)
}

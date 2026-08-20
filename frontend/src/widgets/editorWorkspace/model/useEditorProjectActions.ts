import { type EditorDocument, serializeSvg } from '@entities/editorDocument'
import {
  projectQueryKey,
  projectSvgQueryKey,
  useUpdateProjectMetadataMutation,
} from '@entities/project'
import { useCreateProjectMutation } from '@features/projectCreate'
import { getApiErrorMessage } from '@shared/api/api'
import { useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

interface UseEditorProjectActionsParams {
  canEdit: boolean
  document: EditorDocument
  initialProjectName: string
  projectId: string
}

export function useEditorProjectActions({
  canEdit,
  document,
  initialProjectName,
  projectId,
}: UseEditorProjectActionsParams) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const { t } = useTranslation()
  const [projectName, setProjectName] = useState(initialProjectName)
  const [savedProjectSvg, setSavedProjectSvg] = useState(() =>
    serializeSvg(document),
  )
  const [isCopyingProject, setIsCopyingProject] = useState(false)
  const isLeavingAfterCopyReference = useRef(false)
  const { isPending: isSavingVersion, mutateAsync: saveProjectVersion } =
    useUpdateProjectMetadataMutation()
  const { isPending: isCreatingProjectCopy, mutateAsync: createProject } =
    useCreateProjectMutation()
  const hasUnsavedChanges = serializeSvg(document) !== savedProjectSvg

  const shouldBlockNavigation = useCallback(
    () => canEdit && hasUnsavedChanges && !isLeavingAfterCopyReference.current,
    [canEdit, hasUnsavedChanges],
  )

  const renameProject = useCallback(
    async (nextProjectName: string) => {
      if (nextProjectName === projectName) {
        return
      }

      try {
        const updatedProject = await saveProjectVersion({
          name: nextProjectName,
          projectId,
        })

        setProjectName(updatedProject.name ?? nextProjectName)
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: projectQueryKey(projectId),
          }),
          queryClient.invalidateQueries({ queryKey: ['user-profile'] }),
          queryClient.invalidateQueries({ queryKey: ['feed'] }),
        ])
        toast.info(t('Project name updated.'))
      } catch (error) {
        toast.error(getApiErrorMessage(error))

        throw error
      }
    },
    [projectId, projectName, queryClient, saveProjectVersion, t, toast],
  )

  const saveVersion = useCallback(async () => {
    const projectSvg = serializeSvg(document)

    if (projectSvg === savedProjectSvg) {
      toast.error(t('There are no changes to save.'))

      return
    }

    try {
      await saveProjectVersion({ data: projectSvg, projectId })
      setSavedProjectSvg(projectSvg)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: projectQueryKey(projectId) }),
        queryClient.invalidateQueries({
          queryKey: projectSvgQueryKey(projectId),
        }),
      ])
      toast.info(t('Project version saved.'))
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    }
  }, [
    document,
    projectId,
    queryClient,
    saveProjectVersion,
    savedProjectSvg,
    t,
    toast,
  ])

  const createCopy = useCallback(async () => {
    setIsCopyingProject(true)

    try {
      const createdProject = await createProject({ name: projectName })

      if (!createdProject.id) {
        throw new TypeError('Created project ID is missing')
      }

      await saveProjectVersion({
        data: serializeSvg(document),
        projectId: createdProject.id,
      })
      isLeavingAfterCopyReference.current = true
      navigate(`/projects/${createdProject.id}`, { viewTransition: true })
      toast.info(t('Project copy created.'))
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsCopyingProject(false)
    }
  }, [
    createProject,
    document,
    navigate,
    projectName,
    saveProjectVersion,
    t,
    toast,
  ])

  return {
    createCopy,
    isCreatingCopy: isCopyingProject || isCreatingProjectCopy,
    isSavingVersion,
    projectName,
    renameProject,
    saveVersion,
    shouldBlockNavigation,
  }
}

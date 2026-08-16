import { authStore } from '@app/providers/auth'
import { importSvg } from '@entities/editorDocument'
import { useProjectQuery, useProjectSvgQuery } from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { AsyncState } from '@shared/ui'
import { EditorWorkspace } from '@widgets/editorWorkspace'
import { useEffect, useMemo } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import styles from './EditorProjectPage.module.css'

export function EditorProjectPage() {
  const { projectId } = useParams()
  const projectQuery = useProjectQuery(projectId ?? '')
  const projectSvgQuery = useProjectSvgQuery(
    projectId ?? '',
    projectQuery.data?.url,
  )
  const parsedDocument = useMemo(() => {
    if (!projectSvgQuery.data) {
      return { document: null, error: null, warnings: [] }
    }

    try {
      return { error: null, ...importSvg(projectSvgQuery.data) }
    } catch (error) {
      return { document: null, error, warnings: [] }
    }
  }, [projectSvgQuery.data])

  useEffect(() => {
    if (!import.meta.env.DEV || !projectSvgQuery.data) {
      return
    }

    if (parsedDocument.error) {
      console.error('Unable to parse project SVG', parsedDocument.error)
    }

    if (parsedDocument.warnings.length > 0) {
      console.warn('Project SVG was normalized before parsing', {
        warnings: parsedDocument.warnings,
      })
    }
  }, [parsedDocument, projectSvgQuery.data])

  if (!projectId) {
    return <Navigate replace to="/" />
  }

  const isError =
    projectQuery.isError ||
    projectSvgQuery.isError ||
    !projectQuery.data?.url ||
    Boolean(parsedDocument.error)
  const isPending =
    projectQuery.isPending ||
    (Boolean(projectQuery.data?.url) && projectSvgQuery.isPending)
  const errorMessage = projectQuery.isError
    ? getApiErrorMessage(projectQuery.error)
    : projectSvgQuery.isError
      ? getApiErrorMessage(projectSvgQuery.error)
      : 'Unable to open this SVG in the editor.'

  return (
    <main className={styles.page}>
      <AsyncState
        errorMessage={errorMessage}
        isError={isError}
        isPending={isPending}
        loadingClassName={styles.loadingState}
      >
        {parsedDocument.document && projectQuery.data && (
          <EditorWorkspace
            canEdit={
              projectQuery.data.author_id === authStore.getSession?.user.id
            }
            document={parsedDocument.document}
            key={projectId}
            projectId={projectId}
            projectName={projectQuery.data.name ?? 'Untitled project'}
          />
        )}
      </AsyncState>
    </main>
  )
}

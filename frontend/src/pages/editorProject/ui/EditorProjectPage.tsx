import { parseSvg } from '@entities/editorDocument'
import { useProjectQuery, useProjectSvgQuery } from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { AsyncState } from '@shared/ui'
import { EditorWorkspace } from '@widgets/editorWorkspace'
import { useMemo } from 'react'
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
      return { document: null, error: null }
    }

    try {
      return { document: parseSvg(projectSvgQuery.data), error: null }
    } catch (error) {
      return { document: null, error }
    }
  }, [projectSvgQuery.data])

  if (!projectId) {
    return <Navigate replace to="/" />
  }

  const isError =
    projectQuery.isError ||
    projectSvgQuery.isError ||
    !projectQuery.data?.url ||
    Boolean(parsedDocument.error)
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
        isPending={projectQuery.isPending || projectSvgQuery.isPending}
      >
        {parsedDocument.document && projectQuery.data && (
          <EditorWorkspace
            document={parsedDocument.document}
            key={projectId}
            projectName={projectQuery.data.name ?? 'Untitled project'}
          />
        )}
      </AsyncState>
    </main>
  )
}

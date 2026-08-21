import { type EditorDocument, importSvg } from '@entities/editorDocument'
import {
  getProjectSvg,
  projectQueryKey,
  projectSvgQueryPrefix,
} from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import type { VersionInfo } from '@shared/api/types'
import { useEscAction, useLockBodyScroll } from '@shared/lib'
import {
  Button,
  ConfirmationModal,
  IconButton,
  Portal,
  Spinner,
  useToast,
} from '@shared/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import {
  getProjectVersion,
  getProjectVersions,
  restoreProjectVersion,
} from '../api/versionHistory'
import styles from './VersionHistoryModal.module.css'

const versionListQueryKey = (projectId: string) =>
  ['project-versions', projectId] as const
const versionQueryKey = (projectId: string, versionId: string) =>
  ['project-version', projectId, versionId] as const

interface VersionHistoryModalProps {
  hasUnsavedChanges: boolean
  isOpen: boolean
  onClose: () => void
  onRestoreDocument: (document: EditorDocument) => void
  projectId: string
}

function getVersionLabel(version: VersionInfo) {
  return `Version ${version.version_num ?? '—'}`
}

function formatVersionDate(value: string | undefined, language: string) {
  if (!value) {
    return '—'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '—'
  }

  return new Intl.DateTimeFormat(language, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function VersionHistoryModal({
  hasUnsavedChanges,
  isOpen,
  onClose,
  onRestoreDocument,
  projectId,
}: VersionHistoryModalProps) {
  const { i18n, t } = useTranslation()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [selectedVersion, setSelectedVersion] = useState<VersionInfo | null>(
    null,
  )
  const [isRestoreConfirmationOpen, setIsRestoreConfirmationOpen] =
    useState(false)
  const versionsQuery = useQuery({
    queryKey: versionListQueryKey(projectId),
    queryFn: () => getProjectVersions(projectId),
    enabled: isOpen,
    retry: false,
  })
  const selectedVersionId = selectedVersion?.id
  const versionQuery = useQuery({
    queryKey: versionQueryKey(projectId, selectedVersionId ?? ''),
    queryFn: () => {
      if (!selectedVersionId) {
        throw new TypeError('Version ID is missing')
      }

      return getProjectVersion(projectId, selectedVersionId).then(
        async (version) => {
          if (!version.url) {
            throw new TypeError('Version URL is missing')
          }

          const svg = await getProjectSvg(version.url)

          return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
        },
      )
    },
    enabled: isOpen && Boolean(selectedVersionId),
    retry: false,
  })
  const restoreVersion = useMutation({
    mutationFn: async () => {
      if (!selectedVersionId) {
        throw new TypeError('Version ID is missing')
      }

      const restoredVersion = await restoreProjectVersion(
        projectId,
        selectedVersionId,
      )

      if (!restoredVersion.url) {
        throw new TypeError('Restored version URL is missing')
      }

      const svg = await getProjectSvg(restoredVersion.url)

      return importSvg(svg).document
    },
    onSuccess: async (document) => {
      onRestoreDocument(document)
      await queryClient.invalidateQueries({
        queryKey: versionListQueryKey(projectId),
      })
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: projectQueryKey(projectId) }),
        queryClient.invalidateQueries({
          queryKey: projectSvgQueryPrefix(projectId),
        }),
      ])
      toast.info(t('Version restored.'))
      onClose()
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error))
    },
  })

  useEffect(() => {
    if (!isOpen) {
      setSelectedVersion(null)
      setIsRestoreConfirmationOpen(false)
    }
  }, [isOpen])

  useLockBodyScroll(isOpen)
  useEscAction(handleClose, isOpen && !isRestoreConfirmationOpen)

  if (!isOpen) {
    return null
  }

  const versions = versionsQuery.data ?? []
  const currentVersionId = versions[0]?.id
  const isSelectedCurrentVersion = selectedVersionId === currentVersionId
  const previewUrl = versionQuery.data

  function handleClose() {
    if (!restoreVersion.isPending) {
      onClose()
    }
  }

  return (
    <>
      <Portal>
        <div className={styles.overlay}>
          <button
            className={styles.backdrop}
            onClick={handleClose}
            type="button"
          />
          <section className={styles.modal}>
            <header className={styles.header}>
              <div>
                <h2 className={styles.title}>{t('Version history')}</h2>
                <p className={styles.subtitle}>
                  {t('Choose a saved version to preview or restore.')}
                </p>
              </div>
              <IconButton
                className={styles.closeButton}
                onClick={handleClose}
                size="sm"
              >
                <X />
              </IconButton>
            </header>
            <div className={styles.content}>
              <div className={styles.versionList}>
                {versionsQuery.isPending && (
                  <div className={styles.centered}>
                    <Spinner />
                  </div>
                )}
                {versionsQuery.isError && (
                  <p className={styles.empty}>
                    {t('Unable to load version history. Please try again.')}
                  </p>
                )}
                {!versionsQuery.isPending &&
                  !versionsQuery.isError &&
                  versions.length === 0 && (
                    <p className={styles.empty}>
                      {t('There are no saved versions yet.')}
                    </p>
                  )}
                {versions.map((version) => {
                  const isCurrent = version.id === currentVersionId
                  const isSelected = version.id === selectedVersionId

                  return (
                    <button
                      className={`${styles.versionRow} ${isSelected ? styles.versionRowSelected : ''}`}
                      key={
                        version.id ??
                        `${version.version_num}-${version.created_at}`
                      }
                      onClick={() => setSelectedVersion(version)}
                      type="button"
                    >
                      <span>{getVersionLabel(version)}</span>
                      <span className={styles.versionMeta}>
                        {formatVersionDate(version.created_at, i18n.language)}
                        {isCurrent && (
                          <span className={styles.currentBadge}>
                            {t('Current')}
                          </span>
                        )}
                      </span>
                    </button>
                  )
                })}
              </div>
              <div className={styles.preview}>
                {!selectedVersion && (
                  <p className={styles.empty}>
                    {t('Select a version to see its preview.')}
                  </p>
                )}
                {selectedVersion && versionQuery.isPending && (
                  <div className={styles.centered}>
                    <Spinner />
                  </div>
                )}
                {selectedVersion && versionQuery.isError && (
                  <p className={styles.empty}>
                    {t('Unable to load this version. Please try again.')}
                  </p>
                )}
                {selectedVersion &&
                  previewUrl &&
                  !versionQuery.isPending &&
                  !versionQuery.isError && (
                    <>
                      <div className={styles.previewCanvas}>
                        <img alt="" src={previewUrl} />
                      </div>
                      <div className={styles.previewFooter}>
                        <div>
                          <strong>{getVersionLabel(selectedVersion)}</strong>
                          <p>
                            {formatVersionDate(
                              selectedVersion.created_at,
                              i18n.language,
                            )}
                          </p>
                        </div>
                        {!isSelectedCurrentVersion && (
                          <Button
                            onClick={() => setIsRestoreConfirmationOpen(true)}
                          >
                            {t('Restore version')}
                          </Button>
                        )}
                      </div>
                    </>
                  )}
              </div>
            </div>
          </section>
        </div>
      </Portal>
      {selectedVersion && (
        <ConfirmationModal
          cancelLabel={t('Cancel')}
          confirmLabel={t('Restore version')}
          confirmVariant="primary"
          description={
            hasUnsavedChanges
              ? t(
                  'Your unsaved changes will be replaced. The current saved version will remain in the history.',
                )
              : t(
                  'The current saved version will remain in the history. A new version will be created.',
                )
          }
          isConfirming={restoreVersion.isPending}
          isOpen={isRestoreConfirmationOpen}
          onCancel={() => {
            if (!restoreVersion.isPending) {
              setIsRestoreConfirmationOpen(false)
            }
          }}
          onConfirm={() => restoreVersion.mutate()}
          title={t('Restore this version?')}
        />
      )}
    </>
  )
}

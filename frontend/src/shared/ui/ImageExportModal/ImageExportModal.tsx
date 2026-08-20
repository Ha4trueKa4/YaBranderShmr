import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { Button } from '../Button'
import { Portal } from '../Portal'
import styles from './ImageExportModal.module.css'

const exportFileTypes = [
  {
    description: 'Best for web and transparent backgrounds',
    extension: 'png',
    label: 'PNG',
  },
  {
    description: 'Smaller files for photos and previews',
    extension: 'jpeg',
    label: 'JPEG',
  },
  {
    description: 'Scalable vector format for editing and print',
    extension: 'svg',
    label: 'SVG',
  },
] as const

export type ImageExportFileType = (typeof exportFileTypes)[number]['extension']

export interface ImageExportModalProps {
  isOpen: boolean
  onClose: () => void
  onExport: (fileType: ImageExportFileType) => void
}

export function ImageExportModal({
  isOpen,
  onClose,
  onExport,
}: ImageExportModalProps) {
  const { t } = useTranslation()
  const [fileType, setFileType] = useState<ImageExportFileType>('png')
  const titleId = useId()

  if (!isOpen) {
    return null
  }

  return (
    <Portal>
      <div className={styles.overlay}>
        <button
          aria-label={t('Close export dialog')}
          className={styles.backdrop}
          onClick={onClose}
          type="button"
        />
        <section
          aria-labelledby={titleId}
          aria-modal="true"
          className={styles.modal}
          role="dialog"
        >
          <h2 id={titleId}>{t('Export image')}</h2>
          <p className={styles.description}>
            {t('Choose a file type for export.')}
          </p>
          <div className={styles.fileTypes}>
            {exportFileTypes.map((type) => (
              <label className={styles.fileType} key={type.extension}>
                <input
                  checked={fileType === type.extension}
                  name="image-export-file-type"
                  onChange={() => setFileType(type.extension)}
                  type="radio"
                  value={type.extension}
                />
                <span className={styles.fileTypeContent}>
                  <strong>{type.label}</strong>
                  <p className={styles.text}>{t(type.description)}</p>
                </span>
              </label>
            ))}
          </div>
          <div className={styles.actions}>
            <Button onClick={onClose} variant="secondary">
              {t('Cancel')}
            </Button>
            <Button onClick={() => onExport(fileType)}>{t('Export')}</Button>
          </div>
        </section>
      </div>
    </Portal>
  )
}

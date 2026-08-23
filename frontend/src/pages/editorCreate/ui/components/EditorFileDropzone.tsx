import { GenerateIcon } from '@shared/ui'
import { Upload } from 'lucide-react'
import { type ChangeEvent, type DragEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { getUploadedSvgValidationError } from '../../model/validateUploadedSvg'
import styles from './EditorFileDropzone.module.css'

interface EditorFileDropzoneProps {
  description: string
  onDraggingChange: (isDragging: boolean) => void
  onUpload: (data: string) => void
  title: string
}

export function EditorFileDropzone({
  description,
  onDraggingChange,
  onUpload,
  title,
}: EditorFileDropzoneProps) {
  const { t } = useTranslation()
  const [error, setError] = useState<string>()

  async function uploadFile(file: File | undefined) {
    if (!file) {
      return
    }

    const data = await file.text()
    const validationError = getUploadedSvgValidationError(file.name, data)

    if (validationError) {
      const messages = {
        invalidFileType: t('Only SVG and TXT files are supported.'),
        invalidSvg: t('The file must contain valid SVG markup.'),
        tooLong: t('SVG must not exceed 5000 characters.'),
      }

      setError(messages[validationError])
      return
    }

    setError(undefined)
    onUpload(data)
  }

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    void uploadFile(event.target.files?.[0])
    event.target.value = ''
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    onDraggingChange(false)
    void uploadFile(event.dataTransfer.files[0])
  }

  return (
    <label
      className={styles.dropzone}
      onDragEnter={(event) => {
        event.preventDefault()
        onDraggingChange(true)
      }}
      onDragLeave={(event) => {
        if (event.currentTarget === event.target) {
          onDraggingChange(false)
        }
      }}
      onDragOver={(event) => event.preventDefault()}
      onDrop={handleDrop}
    >
      <input
        accept=".svg,.txt,image/svg+xml,text/plain"
        className={styles.input}
        onChange={handleChange}
        type="file"
      />
      <div className={styles.content}>
        <GenerateIcon className={styles.generateIcon} />
        <h1>{title}</h1>
        <p>{description}</p>
        <span className={styles.uploadHint}>
          <Upload size={16} strokeWidth={2} />
          {t('or drop an existing SVG')}
        </span>
        {error && <p className={styles.error}>{error}</p>}
      </div>
    </label>
  )
}

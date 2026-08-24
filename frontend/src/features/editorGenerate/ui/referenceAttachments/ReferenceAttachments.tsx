import { Paperclip, X } from 'lucide-react'

import styles from './ReferenceAttachments.module.css'

export interface ReferenceAttachment {
  id: string
  name: string
}

interface ReferenceAttachmentsProps {
  onRemove: (referenceId: string) => void
  references: ReferenceAttachment[]
}

export function ReferenceAttachments({
  onRemove,
  references,
}: ReferenceAttachmentsProps) {
  if (references.length === 0) {
    return null
  }

  return (
    <div className={styles.references}>
      {references.map((reference) => (
        <span className={styles.reference} key={reference.id}>
          <Paperclip size={14} />
          <span className={styles.referenceName}>{reference.name}</span>
          <button
            className={styles.removeReferenceButton}
            onClick={(event) => {
              event.preventDefault()
              event.stopPropagation()
              onRemove(reference.id)
            }}
            type="button"
          >
            <X size={14} />
          </button>
        </span>
      ))}
    </div>
  )
}

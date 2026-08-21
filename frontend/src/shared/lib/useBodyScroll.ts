import { useEffect } from 'react'

let lockCount = 0
let initialOverflow = ''

export function useBodyScroll(isLocked: boolean) {
  useEffect(() => {
    if (!isLocked) {
      return
    }

    const { body } = document

    if (lockCount === 0) {
      initialOverflow = body.style.overflow
    }

    lockCount += 1
    body.style.overflow = 'hidden'

    return () => {
      lockCount -= 1

      if (lockCount === 0) {
        body.style.overflow = initialOverflow
      }
    }
  }, [isLocked])
}
